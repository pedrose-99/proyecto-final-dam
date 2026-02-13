from pydantic import BaseModel, Field
from typing import Optional


class ScrapedProduct(BaseModel):
    """Modelo con campos camelCase que mapean directamente al record Java ScrapedProduct."""

    external_id: Optional[str] = Field(None, alias="externalId")
    ean: Optional[str] = None
    name: Optional[str] = None
    brand: Optional[str] = None
    description: Optional[str] = None
    price: Optional[float] = None
    original_price: Optional[float] = Field(None, alias="originalPrice")
    on_sale: bool = Field(False, alias="onSale")
    price_per_unit: Optional[str] = Field(None, alias="pricePerUnit")
    unit: Optional[str] = None
    image_url: Optional[str] = Field(None, alias="imageUrl")
    product_url: Optional[str] = Field(None, alias="productUrl")
    category_name: Optional[str] = Field(None, alias="categoryName")
    category_id: Optional[str] = Field(None, alias="categoryId")
    origin: Optional[str] = None

    model_config = {"populate_by_name": True, "by_alias": True}
