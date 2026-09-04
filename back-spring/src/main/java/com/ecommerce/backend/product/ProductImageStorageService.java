package com.ecommerce.backend.product;

import com.ecommerce.backend.store.Store;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Stockage des visuels, partitionne par boutique.
 *
 * Les fichiers etaient auparavant deposes dans un dossier commun, servi en acces
 * libre : les visuels d'une boutique etaient enumerables depuis n'importe quelle
 * autre. Deux mesures ici, l'arborescence par boutique et des noms non devinables.
 *
 * L'interface reste volontairement etroite (deposer, supprimer, mesurer) pour
 * qu'un passage a un stockage objet ne touche pas au metier.
 */
@Service
public class ProductImageStorageService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final long MAX_STORE_QUOTA_BYTES = 200L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_GIF_VALUE,
            "image/webp"
    );

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Path storageDirectory;

    public ProductImageStorageService(@Value("${app.uploads.directory:uploads/products}") String directory) {
        this.storageDirectory = Paths.get(directory).toAbsolutePath().normalize();
    }

    public String store(Store store, MultipartFile file) {
        validate(file);
        Path storeDirectory = directoryFor(store);
        enforceQuota(storeDirectory, file.getSize(), store);

        try {
            Files.createDirectories(storeDirectory);
            String filename = unpredictableName() + extension(file.getOriginalFilename(), file.getContentType());
            Path target = storeDirectory.resolve(filename).normalize();

            if (!storeDirectory.equals(target.getParent())) {
                throw new IllegalArgumentException("Invalid image filename");
            }

            file.transferTo(target);
            return "/uploads/products/" + store.getId() + "/" + filename;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store product image", exception);
        }
    }

    /**
     * @return true si le fichier appartenait bien a cette boutique et a ete supprime.
     *         Un chemin pointant ailleurs est ignore, jamais suivi.
     */
    public boolean delete(Store store, String imageUrl) {
        Path storeDirectory = directoryFor(store);
        String prefix = "/uploads/products/" + store.getId() + "/";

        if (imageUrl == null || !imageUrl.startsWith(prefix)) {
            return false;
        }

        Path target = storeDirectory.resolve(imageUrl.substring(prefix.length())).normalize();
        if (!storeDirectory.equals(target.getParent())) {
            return false;
        }

        try {
            return Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to delete product image", exception);
        }
    }

    /**
     * Efface tout le dossier d'une boutique. Utilise a sa suppression.
     * Une erreur d'ecriture ne doit pas faire echouer la suppression en base,
     * deja validee : elle est signalee, pas propagee.
     */
    public void deleteAll(Store store) {
        Path storeDirectory = directoryFor(store);
        if (!Files.isDirectory(storeDirectory)) {
            return;
        }

        try (Stream<Path> files = Files.walk(storeDirectory)) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Un fichier verrouille ne doit pas interrompre le nettoyage.
                }
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to clear store images", exception);
        }
    }

    public long usedBytes(Store store) {
        Path storeDirectory = directoryFor(store);
        if (!Files.isDirectory(storeDirectory)) {
            return 0L;
        }

        try (Stream<Path> files = Files.list(storeDirectory)) {
            return files.filter(Files::isRegularFile).mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException exception) {
                    return 0L;
                }
            }).sum();
        } catch (IOException exception) {
            return 0L;
        }
    }

    public long quotaBytes() {
        return MAX_STORE_QUOTA_BYTES;
    }

    private Path directoryFor(Store store) {
        if (store == null || store.getId() == null) {
            throw new IllegalArgumentException("A store is required to store an image");
        }
        // L'identifiant numerique, jamais le slug : un slug est modifiable, ce qui
        // rendrait les visuels deja publies introuvables.
        return storageDirectory.resolve(String.valueOf(store.getId())).normalize();
    }

    private void enforceQuota(Path storeDirectory, long incomingSize, Store store) {
        if (usedBytes(store) + incomingSize > MAX_STORE_QUOTA_BYTES) {
            throw new IllegalArgumentException(
                    "Storage quota exceeded for this store (" + (MAX_STORE_QUOTA_BYTES / (1024 * 1024)) + " MB)");
        }
    }

    /**
     * 128 bits d'aleatoire : le nom ne se devine pas, meme en connaissant
     * l'identifiant de la boutique.
     */
    private String unpredictableName() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Image file must not exceed 5 MB");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Only JPEG, PNG, GIF and WebP images are allowed");
        }
    }

    private String extension(String originalFilename, String contentType) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension != null && !extension.isBlank()) {
            String cleaned = extension.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            if (!cleaned.isBlank()) {
                return "." + cleaned;
            }
        }
        return switch (contentType) {
            case MediaType.IMAGE_PNG_VALUE -> ".png";
            case MediaType.IMAGE_GIF_VALUE -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
