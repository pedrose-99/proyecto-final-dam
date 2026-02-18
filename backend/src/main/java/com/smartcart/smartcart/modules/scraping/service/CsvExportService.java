package com.smartcart.smartcart.modules.scraping.service;

import com.smartcart.smartcart.modules.product.entity.Product;
import com.smartcart.smartcart.modules.product.entity.ProductStore;
import com.smartcart.smartcart.modules.product.repository.ProductStoreRepository;
import com.smartcart.smartcart.modules.store.entity.Store;
import com.smartcart.smartcart.modules.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvExportService
{

    private final ProductStoreRepository productStoreRepository;
    private final StoreRepository storeRepository;

    private static final String CSV_HEADER = "Id,Nombre,Precio,Precio Pack,Formato,Categoria,Supermercado,Url,Url_imagen";

    /**
     * Exporta todos los productos de una tienda a CSV.
     */
    public byte[] exportByStore(String storeSlug)
    {
        Store store = storeRepository.findBySlug(storeSlug)
                .orElseThrow(() -> new IllegalArgumentException("Tienda no encontrada: " + storeSlug));

        List<ProductStore> productStores = productStoreRepository.findAllByStoreWithProductAndCategory(store.getStoreId());
        log.info("[{}] Exportando {} productos a CSV", storeSlug, productStores.size());

        return generateCsv(productStores, store.getName());
    }

    /**
     * Exporta todas las tiendas en un ZIP con un CSV por tienda.
     */
    public byte[] exportAllAsZip()
    {
        List<Store> stores = storeRepository.findAll();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos))
        {
            for (Store store : stores)
            {
                List<ProductStore> productStores = productStoreRepository
                        .findAllByStoreWithProductAndCategory(store.getStoreId());

                if (productStores.isEmpty())
                {
                    log.info("[{}] Sin productos, omitiendo del ZIP", store.getSlug());
                    continue;
                }

                byte[] csv = generateCsv(productStores, store.getName());
                ZipEntry entry = new ZipEntry("products_" + store.getSlug() + ".csv");
                zos.putNextEntry(entry);
                zos.write(csv);
                zos.closeEntry();

                log.info("[{}] Añadido al ZIP: {} productos", store.getSlug(), productStores.size());
            }

            zos.finish();
            return baos.toByteArray();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error generando ZIP de exportación", e);
        }
    }

    private byte[] generateCsv(List<ProductStore> productStores, String storeName)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // BOM para Excel
        try
        {
            baos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8), true);
        writer.println(CSV_HEADER);

        for (ProductStore ps : productStores)
        {
            Product product = ps.getProductId();
            String categoryName = product.getCategoryId() != null ? product.getCategoryId().getName() : "";
            String price = ps.getCurrentPrice() != null ? formatPrice(ps.getCurrentPrice()) : "";
            String unit = product.getUnit() != null ? product.getUnit() : "";

            writer.println(String.join(",",
                    escapeCsv(ps.getExternaId() != null ? ps.getExternaId() : String.valueOf(product.getProductId())),
                    escapeCsv(product.getName() != null ? product.getName() : ""),
                    escapeCsv(price),
                    escapeCsv(""),
                    escapeCsv(unit),
                    escapeCsv(categoryName),
                    escapeCsv(storeName),
                    escapeCsv(ps.getUrl() != null ? ps.getUrl() : ""),
                    escapeCsv(product.getImageUrl() != null ? product.getImageUrl() : "")
            ));
        }

        writer.flush();
        return baos.toByteArray();
    }

    private String formatPrice(Double price)
    {
        if (price == price.intValue())
        {
            return String.valueOf(price.intValue());
        }
        return String.format("%.2f", price);
    }

    private String escapeCsv(String value)
    {
        if (value == null)
        {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n"))
        {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
