import time
import json
import logging
from playwright.sync_api import sync_playwright
from app.scrapers.browser_utils import launch_browser

log = logging.getLogger(__name__)

URL_CATEGORY_CARREFOUR = (
    "https://www.carrefour.es/cloud-api/categories-api/v1/categories/menu/"
)
URL_PRODUCTS_BY_CATEGORY_CARREFOUR = (
    "https://www.carrefour.es/cloud-api/plp-food-papi/v1"
)


def _fetch_json(page, url: str) -> dict:
    """Navega a una URL de API y extrae el JSON de la respuesta."""
    page.goto(url, wait_until="domcontentloaded", timeout=30_000)
    content = page.evaluate("() => document.body.innerText")
    return json.loads(content)


def _get_subcategory_urls(page, category_ids: list[str]) -> list[str]:
    """Obtiene las URLs de subcategorias hoja para cada categoria raiz."""
    sub_urls = []

    for cat_id in category_ids:
        url = (
            "https://www.carrefour.es/cloud-api/categories-api/v1/categories/menu"
            f"?sale_point=005704&depth=1&current_category={cat_id}&limit=3&lang=es&freelink=true"
        )
        try:
            data = _fetch_json(page, url)
            menu = data.get("menu", [])
            _extract_leaf_urls(menu, sub_urls)
        except Exception as e:
            log.warning("Error subcategorias de %s: %s", cat_id, e)

    return sub_urls


def _extract_leaf_urls(nodes: list, result: list):
    """Extrae recursivamente url_rel de nodos hoja."""
    for node in nodes:
        children = node.get("childs", [])
        if children:
            _extract_leaf_urls(children, result)
        else:
            url_rel = node.get("url_rel")
            if url_rel:
                result.append(url_rel)


def _get_categories(page) -> list[str]:
    """Obtiene las subcategorias hoja de supermercado de Carrefour."""
    try:
        data = _fetch_json(page, URL_CATEGORY_CARREFOUR)
    except Exception:
        log.error("No se pudieron obtener las categorias de Carrefour")
        return []

    menu = data.get("menu", [])
    category_ids = []

    for item in menu:
        childs = item.get("childs", [])
        for child in childs:
            url_rel = child.get("url_rel", "")
            child_id = child.get("id")
            if (
                url_rel.startswith("/supermercado")
                and "ofertas" not in url_rel
                and child_id
            ):
                category_ids.append(str(child_id))

    log.info("Carrefour: %d categorias raiz encontradas", len(category_ids))
    sub_urls = _get_subcategory_urls(page, category_ids)
    log.info("Carrefour: %d subcategorias hoja encontradas", len(sub_urls))
    return sub_urls


def _get_products_by_category(
    page, category_url: str, seen_ids: set
) -> list[dict]:
    """Obtiene todos los productos de una categoria con paginacion."""
    products = []
    offset = 0

    while True:
        url = f"{URL_PRODUCTS_BY_CATEGORY_CARREFOUR}{category_url}?offset={offset}"
        try:
            data = _fetch_json(page, url)
            results = data.get("results")
            if results is None:
                break

            if isinstance(results, dict):
                items = results.get("items", [])
            elif isinstance(results, list) and results:
                items = results[0].get("items", [])
            else:
                break

            if not items:
                break

            # Comprobar si el primer producto ya se vio (fin de paginacion)
            first_id = items[0].get("product_id")
            if first_id and str(first_id) in seen_ids:
                break

            for item in items:
                ext_id = str(item.get("product_id", ""))
                if ext_id in seen_ids:
                    continue
                seen_ids.add(ext_id)

                product_url = item.get("url", "")
                if product_url and not product_url.startswith("http"):
                    product_url = "https://www.carrefour.es" + product_url

                images = item.get("images", {})
                image_url = images.get("desktop", "") if isinstance(images, dict) else ""

                products.append({
                    "externalId": ext_id,
                    "name": item.get("name"),
                    "price": item.get("price"),
                    "pricePerUnit": str(item.get("price_per_unit", "")) if item.get("price_per_unit") is not None else None,
                    "unit": item.get("measure_unit"),
                    "categoryName": category_url,
                    "productUrl": product_url,
                    "imageUrl": image_url,
                })

            offset += 24

        except Exception:
            break

    return products


def scrape_carrefour() -> list[dict]:
    """Scraper principal de Carrefour. Devuelve lista de dicts con campos camelCase."""
    log.info("=== Iniciando scraping de Carrefour ===")
    start = time.time()

    with sync_playwright() as p:
        browser, context, page = launch_browser(p)

        try:
            # Navegar para pasar Cloudflare
            log.info("Navegando a carrefour.es para pasar Cloudflare...")
            page.goto(
                "https://www.carrefour.es",
                wait_until="domcontentloaded",
                timeout=60_000,
            )
            time.sleep(3)

            categories = _get_categories(page)
            log.info("Carrefour: %d categorias a procesar", len(categories))

            all_products = []
            seen_ids: set[str] = set()

            for idx, cat in enumerate(categories, 1):
                log.info("[%d/%d] Carrefour — categoria: %s", idx, len(categories), cat)
                products = _get_products_by_category(page, cat, seen_ids)
                all_products.extend(products)
                time.sleep(1)

        finally:
            browser.close()

    elapsed = time.time() - start
    log.info(
        "=== Carrefour completado: %d productos en %d min %d s ===",
        len(all_products), int(elapsed // 60), int(elapsed % 60),
    )

    return all_products
