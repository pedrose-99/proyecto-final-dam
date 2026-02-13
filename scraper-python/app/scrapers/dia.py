import time
import logging
import requests
from playwright.sync_api import sync_playwright
from app.scrapers.browser_utils import launch_browser

log = logging.getLogger(__name__)

URL_CATEGORY_DIA = (
    "https://www.dia.es/api/v1/plp-insight/initial_analytics/"
    "charcuteria-y-quesos/jamon-cocido-lacon-fiambres-y-mortadela/"
    "c/L2001?navigation=L2001"
)
URL_PRODUCTS_BY_CATEGORY_DIA = "https://www.dia.es/api/v1/plp-back/reduced"

HEADERS_BASE = {
    "Accept": (
        "text/html,application/xhtml+xml,application/xml;q=0.9,"
        "image/avif,image/webp,image/apng,*/*;q=0.8,"
        "application/signed-exchange;v=b3;q=0.7"
    ),
    "Accept-Encoding": "gzip, deflate, br",
    "Accept-Language": "es-GB,es;q=0.9",
    "Sec-Ch-Ua": '"Not_A Brand";v="8", "Chromium";v="120", "Google Chrome";v="120"',
    "Sec-Ch-Ua-Mobile": "?0",
    "Sec-Ch-Ua-Platform": '"Windows"',
    "Sec-Fetch-Mode": "navigate",
    "Sec-Fetch-Site": "none",
    "Sec-Fetch-User": "?1",
    "Upgrade-Insecure-Requests": "1",
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    ),
}


def _acquire_cookies() -> str:
    """Abre dia.es con Playwright, espera a que Imperva resuelva y extrae cookies."""
    log.info("Adquiriendo cookies de Dia con Playwright...")
    with sync_playwright() as p:
        browser, context, page = launch_browser(p)
        try:
            page.goto("https://www.dia.es", wait_until="domcontentloaded", timeout=60_000)

            # Esperar a que aparezca la cookie _abck (proteccion Imperva/Akamai)
            for _ in range(30):
                cookies = context.cookies()
                cookie_names = {c["name"] for c in cookies}
                if "_abck" in cookie_names:
                    break
                time.sleep(1)
            else:
                log.warning("Cookie _abck no encontrada, continuando igualmente")

            # Construir cookie string
            cookies = context.cookies()
            cookie_str = "; ".join(f'{c["name"]}={c["value"]}' for c in cookies)
            log.info("Cookies adquiridas: %d cookies", len(cookies))
            return cookie_str
        finally:
            browser.close()


def _make_headers(cookie_str: str) -> dict:
    headers = dict(HEADERS_BASE)
    headers["Cookie"] = cookie_str
    return headers


def _api_get(url: str, headers: dict) -> dict:
    """GET con reintentos de cookies ante 403."""
    resp = requests.get(url, headers=headers, timeout=30)
    if resp.status_code == 403:
        log.warning("403 en %s — re-adquiriendo cookies", url)
        new_cookies = _acquire_cookies()
        headers["Cookie"] = new_cookies
        resp = requests.get(url, headers=headers, timeout=30)
        resp.raise_for_status()
    else:
        resp.raise_for_status()
    return resp.json()


def _procesar_nodo(nodo: dict) -> list[str]:
    """Extrae recursivamente los paths de categorias hoja."""
    paths = []
    for value in nodo.values():
        parameter = value.get("parameter")
        path = value.get("path")
        children = value.get("children", {})
        if children:
            paths.extend(_procesar_nodo(children))
        elif parameter is not None and path is not None:
            paths.append(path)
    return paths


def _get_categories(headers: dict) -> list[str]:
    """Obtiene la lista de paths de categorias de Dia."""
    try:
        data = _api_get(URL_CATEGORY_DIA, headers)
    except Exception as e:
        log.error("Error obteniendo categorias de Dia: %s", e)
        return []

    info = data.get("menu_analytics", {})
    return _procesar_nodo(info)


def _get_products_by_category(
    category_path: str, headers: dict
) -> list[dict]:
    """Obtiene productos de una categoria de Dia como lista de dicts."""
    url = URL_PRODUCTS_BY_CATEGORY_DIA + category_path
    products = []

    try:
        data = _api_get(url, headers)
        items = data.get("plp_items", [])

        for item in items:
            prices = item.get("prices", {})
            price_val = prices.get("price") if isinstance(prices, dict) else item.get("prices_price")
            price_per_unit = prices.get("price_per_unit") if isinstance(prices, dict) else item.get("prices_price_per_unit")
            measure_unit = prices.get("measure_unit") if isinstance(prices, dict) else item.get("prices_measure_unit")

            product_url = item.get("url", "")
            if product_url and not product_url.startswith("http"):
                product_url = "https://www.dia.es" + product_url

            image_url = item.get("image", "")
            if image_url and not image_url.startswith("http"):
                image_url = "https://www.dia.es" + image_url

            products.append({
                "externalId": str(item.get("object_id", "")),
                "name": item.get("display_name"),
                "price": price_val,
                "pricePerUnit": str(price_per_unit) if price_per_unit is not None else None,
                "unit": measure_unit,
                "categoryName": category_path,
                "productUrl": product_url,
                "imageUrl": image_url,
            })
    except Exception as e:
        log.error("Error en categoria %s: %s", category_path, e)

    return products


def scrape_dia() -> list[dict]:
    """Scraper principal de Dia. Devuelve lista de dicts con campos camelCase."""
    log.info("=== Iniciando scraping de Dia ===")
    start = time.time()

    cookie_str = _acquire_cookies()
    headers = _make_headers(cookie_str)

    categories = _get_categories(headers)
    log.info("Dia: %d categorias encontradas", len(categories))

    all_products = []
    seen_ids = set()

    for idx, cat in enumerate(categories, 1):
        log.info("[%d/%d] Dia — categoria: %s", idx, len(categories), cat)
        products = _get_products_by_category(cat, headers)

        for p in products:
            ext_id = p.get("externalId")
            if ext_id and ext_id not in seen_ids:
                seen_ids.add(ext_id)
                all_products.append(p)

        time.sleep(1)

    elapsed = time.time() - start
    log.info("=== Dia completado: %d productos en %d min %d s ===",
             len(all_products), int(elapsed // 60), int(elapsed % 60))

    return all_products
