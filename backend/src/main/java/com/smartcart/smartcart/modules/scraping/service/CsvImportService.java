package com.smartcart.smartcart.modules.scraping.service;

import com.smartcart.smartcart.modules.scraping.dto.ScrapedProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvImportService
{

    private final ProductSyncService productSyncService;

    /**
     * Importa productos desde un CSV (Carrefour o Dia) y los sincroniza con la BD.
     * Columnas esperadas: Id, Nombre, Precio, Precio Pack, Formato, Categoria, Supermercado, Url, Url_imagen
     */
    /**
     * Importa desde una ruta de disco (usado por el endpoint REST).
     */
    public ProductSyncService.SyncResult importFromFile(String filePath, String storeSlug)
    {
        Path path = Path.of(filePath);
        if (!Files.exists(path))
        {
            throw new IllegalArgumentException("File not found: " + filePath);
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            log.info("[{}] Importing CSV from file: {}", storeSlug, filePath);
            return parseAndSync(reader, storeSlug);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error reading CSV file: " + filePath, e);
        }
    }

    /**
     * Importa desde un recurso del classpath (usado en el arranque automatico).
     * Ejemplo: importFromClasspath("data/products_dia.csv", "dia")
     */
    public ProductSyncService.SyncResult importFromClasspath(String classpathResource, String storeSlug)
    {
        InputStream is = getClass().getClassLoader().getResourceAsStream(classpathResource);
        if (is == null)
        {
            throw new IllegalArgumentException("Classpath resource not found: " + classpathResource);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))
        {
            log.info("[{}] Importing CSV from classpath: {}", storeSlug, classpathResource);
            return parseAndSync(reader, storeSlug);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error reading classpath CSV: " + classpathResource, e);
        }
    }

    /**
     * Logica comun de parseo: lee lineas del BufferedReader, parsea y sincroniza.
     */
    private ProductSyncService.SyncResult parseAndSync(BufferedReader reader, String storeSlug) throws IOException
    {
        List<ScrapedProduct> products = new ArrayList<>();
        int lineNumber = 0;

        String headerLine = reader.readLine();
        lineNumber++;

        if (headerLine != null && headerLine.startsWith("\uFEFF"))
        {
            headerLine = headerLine.substring(1);
        }

        log.info("[{}] Header: {}", storeSlug, headerLine);

        String line;
        while ((line = reader.readLine()) != null)
        {
            lineNumber++;
            if (line.isBlank())
            {
                continue;
            }

            try
            {
                List<String> fields = parseCsvLine(line);
                if (fields.size() < 9)
                {
                    log.warn("Line {} has {} fields (expected 9), skipping: {}", lineNumber, fields.size(), line);
                    continue;
                }

                ScrapedProduct product = mapToScrapedProduct(fields, storeSlug);
                if (product != null)
                {
                    products.add(product);
                }
            }
            catch (Exception e)
            {
                log.warn("Error parsing line {}: {} - {}", lineNumber, e.getMessage(), line);
            }
        }

        log.info("[{}] Parsed {} products from CSV, syncing...", storeSlug, products.size());
        return productSyncService.syncProducts(products, storeSlug);
    }

    private ScrapedProduct mapToScrapedProduct(List<String> fields, String storeSlug)
    {
        String id = fields.get(0).trim();
        String name = fields.get(1).trim();
        String priceStr = fields.get(2).trim();
        String pricePackStr = fields.get(3).trim();
        String format = fields.get(4).trim();
        String categoryUrl = fields.get(5).trim();
        // fields.get(6) = Supermercado (lo ignoramos)
        String url = fields.get(7).trim();
        String imageUrl = fields.get(8).trim();

        BigDecimal price = parsePrice(priceStr);
        if (price == null)
        {
            return null;
        }

        String pricePerUnit = parsePrice(pricePackStr) != null ? pricePackStr : null;
        String categoryName = parseCategoryFromUrl(categoryUrl, storeSlug);

        return ScrapedProduct.builder()
                .externalId(id)
                .name(name)
                .price(price)
                .pricePerUnit(pricePerUnit)
                .unit(format)
                .categoryName(categoryName)
                .productUrl(url)
                .imageUrl(imageUrl)
                .build();
    }

    /**
     * Parsea precios en distintos formatos:
     * - Carrefour: "1,50 €", "23 €"
     * - Dia: 3.45
     */
    private BigDecimal parsePrice(String priceStr)
    {
        if (priceStr == null || priceStr.isBlank())
        {
            return null;
        }

        try
        {
            String cleaned = priceStr
                    .replace("\"", "")
                    .replace("€", "")
                    .replace("\u00a0", "")
                    .trim()
                    .replace(",", ".");

            if (cleaned.isEmpty())
            {
                return null;
            }

            return new BigDecimal(cleaned);
        }
        catch (NumberFormatException e)
        {
            log.debug("Could not parse price: '{}'", priceStr);
            return null;
        }
    }

    /**
     * Extrae nombre legible de categoria desde la URL.
     * - Carrefour: /supermercado/el-mercado-promocion/F-10flZ13rjo/c → "El Mercado Promocion"
     * - Dia: /charcuteria-y-quesos/jamon-cocido-pavo-y-pollo/c/L2001 → "Jamon Cocido Pavo Y Pollo"
     */
    private String parseCategoryFromUrl(String categoryUrl, String storeSlug)
    {
        if (categoryUrl == null || categoryUrl.isBlank())
        {
            return null;
        }

        try
        {
            String[] segments = categoryUrl.split("/");

            if ("dia".equals(storeSlug))
            {
                // /charcuteria-y-quesos/jamon-cocido-pavo-y-pollo/c/L2001
                // Buscamos el segmento justo antes de "c"
                for (int i = segments.length - 1; i >= 0; i--)
                {
                    if ("c".equals(segments[i]) && i > 0)
                    {
                        return formatCategoryName(segments[i - 1]);
                    }
                }
            }
            else if ("carrefour".equals(storeSlug))
            {
                // /supermercado/el-mercado-promocion/F-10flZ13rjo/c
                // Tomamos el segmento después de "supermercado"
                for (int i = 0; i < segments.length; i++)
                {
                    if ("supermercado".equals(segments[i]) && i + 1 < segments.length)
                    {
                        return formatCategoryName(segments[i + 1]);
                    }
                }
            }

            // Fallback: tomar el segmento más largo que no parezca un código
            String best = null;
            for (String segment : segments)
            {
                if (!segment.isBlank() && segment.contains("-") &&
                    (best == null || segment.length() > best.length()))
                {
                    best = segment;
                }
            }

            return best != null ? formatCategoryName(best) : null;
        }
        catch (Exception e)
        {
            log.debug("Could not parse category from URL: '{}'", categoryUrl);
            return null;
        }
    }

    /**
     * "jamon-cocido-pavo-y-pollo" → "Jamon Cocido Pavo Y Pollo"
     */
    private String formatCategoryName(String slug)
    {
        if (slug == null || slug.isBlank())
        {
            return null;
        }

        String[] words = slug.replace("-", " ").split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words)
        {
            if (!word.isEmpty())
            {
                if (sb.length() > 0)
                {
                    sb.append(" ");
                }
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1)
                {
                    sb.append(word.substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }

    /**
     * Parsea una línea CSV respetando campos entrecomillados (RFC 4180).
     */
    private List<String> parseCsvLine(String line)
    {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++)
        {
            char c = line.charAt(i);

            if (inQuotes)
            {
                if (c == '"')
                {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"')
                    {
                        current.append('"');
                        i++;
                    }
                    else
                    {
                        inQuotes = false;
                    }
                }
                else
                {
                    current.append(c);
                }
            }
            else
            {
                if (c == '"')
                {
                    inQuotes = true;
                }
                else if (c == ',')
                {
                    fields.add(current.toString());
                    current.setLength(0);
                }
                else
                {
                    current.append(c);
                }
            }
        }

        fields.add(current.toString());
        return fields;
    }
}
