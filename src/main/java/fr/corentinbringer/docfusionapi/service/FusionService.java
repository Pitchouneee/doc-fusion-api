package fr.corentinbringer.docfusionapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FusionService {

    private final ObjectMapper objectMapper;

    public ByteArrayInputStream replacePlaceholders(InputStream docxInputStream, String jsonData) throws IOException {
        Map<String, Object> dataMap = objectMapper.readValue(jsonData, Map.class);

        try (XWPFDocument document = new XWPFDocument(docxInputStream)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                // Gather all the text from the paragraph to handle fragmented placeholders
                StringBuilder paragraphText = new StringBuilder();
                List<XWPFRun> runs = paragraph.getRuns();
                for (XWPFRun run : runs) {
                    paragraphText.append(run.getText(0) != null ? run.getText(0) : "");
                }

                // Replace simple and nested fiedls within the entire text
                String replacedText = replaceNestedFields(paragraphText.toString(), dataMap);

                // Reapply the modified text on the existing XWPFRun objects to preserve styles
                int currentPos = 0;
                for (XWPFRun run : runs) {
                    String originalText = run.getText(0);
                    if (originalText != null) {
                        int length = originalText.length();

                        // Ensure it does not exceed the length of replacedText
                        if (currentPos < replacedText.length()) {
                            String newText = replacedText.substring(currentPos, Math.min(currentPos + length, replacedText.length()));
                            run.setText(newText, 0); // Replace the text while preserving the style of each XWPFRun
                            currentPos += length;
                        } else {
                            // If the end of replacedText is exceeded, remove the text in XWPFRun
                            run.setText("", 0);
                        }
                    }
                }

                // Remove the remaining XWPFRun objects if the replaced text is shorter than the original
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
                // Recursive call to handle nested levels
                text = replaceFieldsRecursive(text, fullKey, nestedMap);
            } else {
                // Replacement for simple fields
                text = replaceField(text, fullKey, value);
            }
        }

        return text;
    }

    private String replaceField(String text, String key, Object value) {
        return text.replace("{" + key + "}", value != null ? value.toString() : "");
    }
}
