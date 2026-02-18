import time
import asyncio
import logging
from urllib.parse import urlparse, parse_qs, urlencode, urlunparse

import aiohttp
from bs4 import BeautifulSoup

log = logging.getLogger(__name__)

BASE_URL = "https://www.ahorramas.com"

CATEGORY_URLS = [
    f"{BASE_URL}/frescos/",
    f"{BASE_URL}/alimentacion/",
    f"{BASE_URL}/bebidas/",
    f"{BASE_URL}/lacteos/",
    f"{BASE_URL}/congelados/",
]

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    ),
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "es-ES,es;q=0.9",
}

# Concurrencia maxima de peticiones simultaneas
MAX_CONCURRENT = 10

# Productos por pagina (SFCC permite sz alto)
PAGE_SIZE = 1000


def _force_page_size(url: str, sz: int = PAGE_SIZE) -> str:
    """Modifica el parametro sz= de una URL SFCC para pedir mas productos."""
    parsed = urlparse(url)
    params = parse_qs(parsed.query)
    params["sz"] = [str(sz)]
    params["start"] = ["0"]
    new_query = urlencode(params, doseq=True)
    return urlunparse(parsed._replace(query=new_query))


def _parse_tiles(soup: BeautifulSoup) -> list[dict]:
    """Extrae productos de los .product-tile de un documento BeautifulSoup."""
    products = []

    for tile in soup.select(".product-tile"):
        pid_el = tile.select_one("[data-pid]")
        pid = pid_el["data-pid"].strip() if pid_el else ""
        if not pid:
            continue

        brand = tile.get("data-brand", "")

        cats = []
        for i in range(1, 6):
            cat = tile.get(f"data-category{i}", "")
            if cat:
                cats.append(cat)
        category_name = " > ".join(cats) if cats else ""

        name_el = tile.select_one(".pdp-link a")
        name = name_el.get_text(strip=True) if name_el else ""

        price = None
        price_el = tile.select_one(".sales .value[content]")
        if price_el:
            try:
                price = float(price_el["content"])
            except (ValueError, TypeError):
                pass

        original_price = None
        orig_el = tile.select_one(".strike-through .value[content]")
        if orig_el:
            try:
                original_price = float(orig_el["content"])
            except (ValueError, TypeError):
                pass

        on_sale = original_price is not None and price is not None and price < original_price

        product_url = None
        link_el = tile.select_one("a.product-pdp-link[href]")
        if link_el:
            href = link_el["href"]
            product_url = href if href.startswith("http") else BASE_URL + href

        image_url = None
        img_el = tile.select_one("img.tile-image[src]")
        if img_el:
            src = img_el["src"]
            image_url = src if src.startswith("http") else BASE_URL + src

        price_per_unit = None
        ppu_el = tile.select_one(".unit-price-per-unit")
        if ppu_el:
            price_per_unit = ppu_el.get_text(strip=True)

        products.append({
            "externalId": pid,
            "name": name or f"Producto {pid}",
            "brand": brand or None,
            "price": price,
            "originalPrice": original_price,
            "onSale": on_sale,
            "pricePerUnit": price_per_unit,
            "categoryName": category_name or None,
            "productUrl": product_url,
            "imageUrl": image_url,
        })

    return products


async def _fetch(session: aiohttp.ClientSession, sem: asyncio.Semaphore, url: str) -> str | None:
    """Descarga una URL con control de concurrencia."""
    async with sem:
        try:
            async with session.get(url, timeout=aiohttp.ClientTimeout(total=30)) as resp:
                if resp.status != 200:
                    log.warning("HTTP %d para %s", resp.status, url)
                    return None
                return await resp.text()
        except Exception as e:
            log.error("Error descargando %s: %s", url, e)
            return None


async def _get_subcategories(session: aiohttp.ClientSession, sem: asyncio.Semaphore, category_url: str) -> list[str]:
    """Extrae URLs de subcategorias desde una categoria raiz."""
    html = await _fetch(session, sem, category_url)
    if not html:
        return [category_url]

    soup = BeautifulSoup(html, "lxml")
    sub_links = set()

    for a in soup.select("a[href]"):
        href = a["href"]
        if href.startswith("/") and not href.startswith("//"):
            href = BASE_URL + href

        if (href.startswith(category_url)
                and href != category_url
                and href.endswith("/")
                and "/ofertas/" not in href):
            sub_links.add(href)

    if not sub_links:
        return [category_url]

    return sorted(sub_links)


async def _get_products_from_category(session: aiohttp.ClientSession, sem: asyncio.Semaphore, url: str) -> list[dict]:
    """Obtiene todos los productos de una subcategoria.

    Intenta obtener todo en una sola peticion con sz grande.
    Si hay boton 'show more', sigue paginando.
    """
    # Primera peticion: intentar obtener todo de golpe
    html = await _fetch(session, sem, url)
    if not html:
        return []

    soup = BeautifulSoup(html, "lxml")

    # Buscar si hay un boton "show more" con data-url para sacar la URL paginada
    show_more = soup.select_one(".show-more button[data-url]")
    if show_more:
        # Rehacer la peticion con sz grande para traer todo de una vez
        paginated_url = show_more["data-url"]
        big_url = _force_page_size(paginated_url, PAGE_SIZE)
        html2 = await _fetch(session, sem, big_url)
        if html2:
            soup2 = BeautifulSoup(html2, "lxml")
            page2_products = _parse_tiles(soup2)
            # Combinar primera pagina + lo que devolvio la peticion grande
            first_products = _parse_tiles(soup)
            all_products = first_products + page2_products

            # Deduplicar
            seen = set()
            unique = []
            for p in all_products:
                eid = p["externalId"]
                if eid not in seen:
                    seen.add(eid)
                    unique.append(p)

            short = url.split(".com")[-1]
            log.info("  %s -> %d productos", short, len(unique))
            return unique

    products = _parse_tiles(soup)
    short = url.split(".com")[-1]
    log.info("  %s -> %d productos", short, len(products))
    return products


async def _scrape_async() -> list[dict]:
    """Funcion async principal del scraping."""
    sem = asyncio.Semaphore(MAX_CONCURRENT)

    connector = aiohttp.TCPConnector(limit=MAX_CONCURRENT, ttl_dns_cache=300)
    async with aiohttp.ClientSession(headers=HEADERS, connector=connector) as session:

        # Fase 1: obtener subcategorias de todas las categorias en paralelo
        sub_tasks = [_get_subcategories(session, sem, url) for url in CATEGORY_URLS]
        sub_results = await asyncio.gather(*sub_tasks, return_exceptions=True)

        all_subcategories: list[str] = []
        seen_urls: set[str] = set()
        for i, result in enumerate(sub_results):
            if isinstance(result, Exception):
                log.error("Error obteniendo subcategorias de %s: %s", CATEGORY_URLS[i], result)
                continue
            for url in result:
                if url not in seen_urls:
                    seen_urls.add(url)
                    all_subcategories.append(url)

        log.info("Total subcategorias a scrapear: %d", len(all_subcategories))

        # Fase 2: scrapear todas las subcategorias en paralelo
        product_tasks = [_get_products_from_category(session, sem, url) for url in all_subcategories]
        product_results = await asyncio.gather(*product_tasks, return_exceptions=True)

    # Deduplicar productos globalmente
    all_products: list[dict] = []
    seen_ids: set[str] = set()

    for result in product_results:
        if isinstance(result, Exception):
            log.error("Error scrapeando subcategoria: %s", result)
            continue
        for p in result:
            ext_id = p.get("externalId")
            if ext_id and ext_id not in seen_ids:
                seen_ids.add(ext_id)
                all_products.append(p)

    return all_products


async def scrape_ahorramas_async() -> list[dict]:
    """Scraper async principal de Ahorramas."""
    log.info("=== Iniciando scraping de Ahorramas ===")
    start = time.time()

    all_products = await _scrape_async()

    elapsed = time.time() - start
    log.info("=== Ahorramas completado: %d productos unicos en %d min %d s ===",
             len(all_products), int(elapsed // 60), int(elapsed % 60))

    return all_products


def scrape_ahorramas() -> list[dict]:
    """Wrapper sincrono (para uso fuera de un event loop)."""
    return asyncio.run(scrape_ahorramas_async())
