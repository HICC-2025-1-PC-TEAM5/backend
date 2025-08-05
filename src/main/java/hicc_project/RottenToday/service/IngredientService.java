package hicc_project.RottenToday.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
public class IngredientService {


    public String detectIngredient(MultipartFile file) throws IOException, InterruptedException {


        String contentType = file.getContentType();

        Path tempDir = Files.createTempDirectory("upload_");
        Path heicPath = tempDir.resolve("input.heic");
        Path jpgPath = tempDir.resolve("output.jpg");

        File heicFile = new File(heicPath.toString());
        File jpgFile = new File(jpgPath.toString());

        if (List.of("image/jpeg", "image/jpg").contains(contentType)) {
            jpgFile.getParentFile().mkdirs();
            file.transferTo(jpgFile);
        } else if (List.of("image/heic", "image/heif").contains(contentType)) {
            heicFile.getParentFile().mkdirs();
            file.transferTo(heicFile);
            jpgFile.getParentFile().mkdirs();
            ProcessBuilder pb = new ProcessBuilder("magick", heicFile.getAbsolutePath(), jpgFile.getAbsolutePath());
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("이미지 변환 실패");
            }
        } else {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다.");
        }


        String response = sendtoOCRApi(jpgFile);

        heicFile.delete();
        jpgFile.delete();

        return response;
    }

    private static String sendtoOCRApi(File jpgFile) {
        String apiURL = "https://nn03butcil.apigw.ntruss.com/custom/v1/44921/08846e81b3a87fd9cc39d4023a75dcff47a6588055793a5f065212e668c94fe6/general";
        String secretKey = "aG50ckFMUkVTSUhxUXVHSG9waFNvRG9yZ1pDbUpTSU8=";


        try {
            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setUseCaches(false);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setReadTimeout(30000);
            con.setRequestMethod("POST");
            String boundary = "----" + UUID.randomUUID().toString().replaceAll("-", "");
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            con.setRequestProperty("X-OCR-SECRET", secretKey);

            JSONObject json = new JSONObject();
            json.put("version", "V2");
            json.put("requestId", UUID.randomUUID().toString());
            json.put("timestamp", System.currentTimeMillis());
            JSONObject image = new JSONObject();
            image.put("format", "jpg");
            image.put("name", "demo");
            JSONArray images = new JSONArray();
            images.put(image);
            json.put("images", images);
            String postParams = json.toString();

            con.connect();
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            long start = System.currentTimeMillis();
            writeMultiPart(wr, postParams, jpgFile, boundary);
            wr.close();

            int responseCode = con.getResponseCode();
            BufferedReader br;
            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();

            return response.toString();
        } catch (Exception e) {
            throw new RuntimeException("이미지 변환 실패");
        }

    }


    private static void writeMultiPart(OutputStream out, String jsonMessage, File file, String boundary) throws
            IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition:form-data; name=\"message\"\r\n\r\n");
        sb.append(jsonMessage);
        sb.append("\r\n");

        out.write(sb.toString().getBytes("UTF-8"));
        out.flush();

        if (file != null && file.isFile()) {
            out.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
            StringBuilder fileString = new StringBuilder();
            fileString
                    .append("Content-Disposition:form-data; name=\"file\"; filename=");
            fileString.append("\"" + file.getName() + "\"\r\n");
            fileString.append("Content-Type: application/octet-stream\r\n\r\n");
            out.write(fileString.toString().getBytes("UTF-8"));
            out.flush();

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
                out.write("\r\n".getBytes());
            }

            out.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
        }
        out.flush();
    
    }
}
