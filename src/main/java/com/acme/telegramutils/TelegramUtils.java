package com.acme.telegramutils;


import com.acme.TeleIrcBridgeRoutes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.component.telegram.model.IncomingPhotoSize;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;


@Component
public class TelegramUtils
{
    private final Logger LOG = LoggerFactory.getLogger(TeleIrcBridgeRoutes.class);

    @Value("local.file-path")
    private String localFilePath;

    @Value("${publish.url}")
    private String publishUrl;

    @Value("${telegram.token}")
    private String telegramToken;

    public String downloadPhotos(List<IncomingPhotoSize> photoSizes)
    {
        String publishUrlAbsPath = "";

        IncomingPhotoSize photo = photoSizes.stream()
                .max(Comparator.comparing(IncomingPhotoSize::getFileSize))
                .orElse(new IncomingPhotoSize());

        if (photo.getFileSize() == 0) {
            LOG.warn("[Telegram] Eventually photo was null");
            return "";
        }

        /* Fetching images is a tad.. interesting. At first:
         * 1. We get a regular message with a photo attribute set,
         *    which is a list of photo sizes.
         * 2. We pick which size of the image we want and do a query to URL:
         *    https://api.telegram.org/bot{telegram_token}/getFile?file_id={file_id}"
         * 3. Now, the return value contains yet another JSON which has
         *    file_path attribute that can be used to fetch the image, finally.
         * 4. Hence, perform query to URL:
         *    https://api.telegram.org/file/bot{telegram_token}/{file_path}
         */

        try {
            String filePath = "";
            ObjectMapper objectMapper = new ObjectMapper();
            URL photoMetaUrl = new URL(String.format(
                "https://api.telegram.org/bot%s/getFile?file_id=%s",
                telegramToken,
                photo.getFileId()));
            JsonNode photoMetaJson = objectMapper.readTree(photoMetaUrl);
            filePath = photoMetaJson.get("result").get("file_path").asText("");
            URL photoUrl = new URL(String.format(
                "https://api.telegram.org/file/bot%s/%s",
                telegramToken,
                filePath));
            LOG.info("[Telegram] downloading image: {}",
                photoUrl);

            String fileExt = FilenameUtils.getExtension(filePath);
            String localFileAbsPath = String.format("%s/%s.%s",
                localFilePath,
                photo.getFileId(),
                fileExt);
            FileUtils.copyURLToFile(photoUrl, new File(localFileAbsPath));
            publishUrlAbsPath = String.format("%s/%s.png", publishUrl,
                photo.getFileId());
        } catch (MalformedURLException e) {
            LOG.warn("Error generating URL: {}", e);
        } catch (IOException e) {
            LOG.warn("Error opening file: {}", e);
        }

        return publishUrlAbsPath;
    }

}
