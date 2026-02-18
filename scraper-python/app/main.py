import logging
from fastapi import FastAPI
from app.models import ScrapedProduct
from app.scrapers.dia import scrape_dia
from app.scrapers.carrefour import scrape_carrefour
from app.scrapers.ahorramas import scrape_ahorramas_async

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
)

app = FastAPI(title="SmartCart Scraper Python")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/scrape/dia", response_model=list[ScrapedProduct])
def scrape_dia_endpoint():
    products = scrape_dia()
    return [ScrapedProduct(**p) for p in products]


@app.post("/scrape/carrefour", response_model=list[ScrapedProduct])
def scrape_carrefour_endpoint():
    products = scrape_carrefour()
    return [ScrapedProduct(**p) for p in products]


@app.post("/scrape/ahorramas", response_model=list[ScrapedProduct])
async def scrape_ahorramas_endpoint():
    products = await scrape_ahorramas_async()
    return [ScrapedProduct(**p) for p in products]
