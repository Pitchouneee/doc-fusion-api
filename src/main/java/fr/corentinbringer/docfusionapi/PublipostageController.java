package fr.corentinbringer.docfusionapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/publipostage")
@RequiredArgsConstructor
public class PublipostageController {

    private final RestTemplate restTemplate;

    @PostMapping(value = "/generate-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> generatePdf(@RequestParam("file") MultipartFile file, @RequestParam("data") String jsonData) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> dataMap = mapper.readValue(jsonData, Map.class);

            ByteArrayInputStream modifiedDocxStream = replacePlaceholders(file.getInputStream(), dataMap);

            String url = "http://localhost:8080/lool/convert-to/pdf";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("data", new ByteArrayResource(modifiedDocxStream.readAllBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, byte[].class);

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private ByteArrayInputStream replacePlaceholders(InputStream docxInputStream, Map<String, Object> dataMap) throws IOException {
        try (XWPFDocument document = new XWPFDocument(docxInputStream)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                // Rassembler tout le texte du paragraphe pour gérer les balises fragmentées
                StringBuilder paragraphText = new StringBuilder();
                List<XWPFRun> runs = paragraph.getRuns();
                for (XWPFRun run : runs) {
                    paragraphText.append(run.getText(0) != null ? run.getText(0) : "");
                }

                // Remplacer les champs simples et imbriqués dans le texte complet
                String replacedText = replaceNestedFields(paragraphText.toString(), dataMap);

                // Réappliquer le texte modifié sur les `XWPFRun` existants pour conserver les styles
                int currentPos = 0;
                for (XWPFRun run : runs) {
                    String originalText = run.getText(0);
                    if (originalText != null) {
                        int length = originalText.length();

                        // Vérifier que nous ne dépassons pas la longueur de `replacedText`
                        if (currentPos < replacedText.length()) {
                            String newText = replacedText.substring(currentPos, Math.min(currentPos + length, replacedText.length()));
                            run.setText(newText, 0); // Remplacer le texte en conservant le style de chaque `XWPFRun`
                            currentPos += length;
                        } else {
                            // Si nous avons déjà dépassé la fin de `replacedText`, supprimer le texte dans ce `XWPFRun`
                            run.setText("", 0);
                        }
                    }
                }

                // Supprimer les `XWPFRun` restants si le texte remplacé est plus court que l'original
                while (currentPos < replacedText.length() && paragraph.getRuns().size() > runs.size()) {
                    paragraph.removeRun(runs.size());
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.write(outputStream);

            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }

    private String replaceNestedFields(String text, Map<String, Object> dataMap) {
        return replaceFieldsRecursive(text, "", dataMap);
    }

    private String replaceFieldsRecursive(String text, String currentKey, Map<String, Object> currentMap) {
        for (Map.Entry<String, Object> entry : currentMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            String fullKey = currentKey.isEmpty() ? key : currentKey + "." + key;

            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                // Appel récursif pour gérer les niveaux d'imbrication
                text = replaceFieldsRecursive(text, fullKey, nestedMap);
            } else {
                // Remplacement direct pour les champs simples
                text = replaceField(text, fullKey, value);
            }
        }

        return text;
    }

    private String replaceField(String text, String key, Object value) {
        return text.replace("{" + key + "}", value != null ? value.toString() : "");
    }
}
