package hicc_project.RottenToday.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hicc_project.RottenToday.dto.*;
import hicc_project.RottenToday.entity.*;
import hicc_project.RottenToday.repository.IngredientRepository;
import hicc_project.RottenToday.repository.MemberRepository;
import hicc_project.RottenToday.repository.RefrigeratorIngredientRepository;
import jakarta.persistence.EntityNotFoundException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class IngredientService {

    RefrigeratorIngredientRepository refrigeratorIngredientRepository;
    IngredientRepository ingredientRepository;
    MemberRepository memberRepository;
    OpenAiService openAiService;

    @Autowired
    public IngredientService(RefrigeratorIngredientRepository refrigeratorIngredientRepository, IngredientRepository ingredientRepository, MemberRepository memberRepository, OpenAiService openApiService) {
        this.refrigeratorIngredientRepository = refrigeratorIngredientRepository;
        this.ingredientRepository = ingredientRepository;
        this.memberRepository = memberRepository;
        this.openAiService = openAiService;
    }




    public RefrigeratorIngredientResponse getRefidge(Long memberId) {
        List<RefrigeratorIngredient> findRefrigeIngredients = refrigeratorIngredientRepository.findByMemberId(memberId);
        List<RefridgeDto> dtos = new ArrayList<>();
        for (RefrigeratorIngredient refrigeIngredient : findRefrigeIngredients) {
            RefridgeDto refridgeDto = new RefridgeDto(refrigeIngredient);
            dtos.add(refridgeDto);
        }
        RefrigeratorIngredientResponse response = new RefrigeratorIngredientResponse(dtos);
        return response;
    }

    @Transactional
    public void addRefridgeIngredient(Long memberId, RefrigeratorIngredientResponse request) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new EntityNotFoundException("해당 유저 존재x"));
        for (RefridgeDto dto : request.getRefrigeratorIngredient()) {
            RefrigeratorIngredient refrigeratorIngredient = new RefrigeratorIngredient(dto);
            if (ingredientRepository.findByName(dto.getName()).isPresent()) {
                Ingredient ingredient = ingredientRepository.findByName(dto.getName()).get();
                refrigeratorIngredient.setIngredient(ingredient);
                refrigeratorIngredient.setCategory(ingredient.getCategory());
            }
            refrigeratorIngredient.setMember(member);
            int plusDays = measureExpireDate(refrigeratorIngredient.getCategory(), refrigeratorIngredient.getType());
            LocalDateTime expireDate = refrigeratorIngredient.getInput_date().plusDays(plusDays);

            System.out.println(expireDate);
            refrigeratorIngredient.setExpire_date(expireDate);

            refrigeratorIngredientRepository.save(refrigeratorIngredient);
        }
    }

    public void updateRefridgeIngredient(Long memberId, RefridgeIngredientRequest request) {
        List<RefrigeratorIngredient> findIngredients = refrigeratorIngredientRepository.findByMemberId(memberId);
        for (RefrigeratorIngredient refrigeratorIngredient : findIngredients) {
            if (refrigeratorIngredient.getId().equals(request.getRefrigeratorIngredientId())) {
                if (request.getQuantity() == 0) {
                    refrigeratorIngredientRepository.delete(refrigeratorIngredient);
                } else {
                    refrigeratorIngredient.setQuantity(request.getQuantity());
                }
            }
        }
    }

    public void deleteIngredient(Long memberId, Long refridgeId) {
        List<RefrigeratorIngredient> findIngredients = refrigeratorIngredientRepository.findByMemberId(memberId);
        for (RefrigeratorIngredient refrigeratorIngredient : findIngredients) {
            if (refrigeratorIngredient.getId().equals(refridgeId)) {
                refrigeratorIngredientRepository.delete(refrigeratorIngredient);
            }
        }
    }

    private int measureExpireDate(Category category, StorageCondition condition) {
        int expireDate = 0;

        switch (category) {
            case VEGETABLE : if (condition == StorageCondition.NORMAL) {expireDate = 1; break;}
            else if (condition == StorageCondition.REFRIGERATED) {expireDate = 5; break;}
            else if (condition == StorageCondition.FROZEN) {expireDate = 25; break;}
            break;
            case FRUIT: if (condition == StorageCondition.NORMAL) {expireDate = 5; break;}
                else if (condition == StorageCondition.REFRIGERATED) {expireDate = 10; break;}
                else if (condition == StorageCondition.FROZEN) {expireDate = 60; break;}
                break;
            case GRAIN: if (condition == StorageCondition.NORMAL) {expireDate = 60; break;}
                else if (condition == StorageCondition.REFRIGERATED) {expireDate = 120; break;}
                else if (condition == StorageCondition.FROZEN) {expireDate = 270; break;}
                break;
            case MEAT: if (condition == StorageCondition.NORMAL) {break;}
                else if (condition == StorageCondition.REFRIGERATED) {expireDate = 3; break;}
                else if (condition == StorageCondition.FROZEN) {expireDate = 240; break;}
                break;
            case SEAFOOD: if (condition == StorageCondition.NORMAL) {break;}
                else if (condition == StorageCondition.REFRIGERATED) {expireDate = 2; break;}
                else if (condition == StorageCondition.FROZEN) {expireDate = 120; break;}
                break;
            case EGG: if (condition == StorageCondition.NORMAL) {expireDate = 14; break;}
                else if (condition == StorageCondition.REFRIGERATED) {expireDate = 30; break;}
                else if (condition == StorageCondition.FROZEN) {expireDate = 270; break;}
                break;
            case DAIRY: if (condition == StorageCondition.NORMAL) {break;}
                else if (condition == StorageCondition.REFRIGERATED) {expireDate = 7; break;}
                else if (condition == StorageCondition.FROZEN) {expireDate = 45; break;}
                break;
            case BEANS: if (condition == StorageCondition.NORMAL) {expireDate = 270; break;}
                else if (condition == StorageCondition.REFRIGERATED) {break;}
                else if (condition == StorageCondition.FROZEN) {break;}
                break;
            case OIL: if (condition == StorageCondition.NORMAL) {expireDate = 270; break;}
                else if (condition == StorageCondition.REFRIGERATED) {break;}
                else if (condition == StorageCondition.FROZEN) {break;}
                break;
            case CONDIMENT: if (condition == StorageCondition.NORMAL) {expireDate = 1000; break;}
                else if (condition == StorageCondition.REFRIGERATED) {break;}
                else if (condition == StorageCondition.FROZEN) {break;}
                break;
            case PROCESSED: break;
            case DRINK: if (condition == StorageCondition.NORMAL) {expireDate = 135; break;}
                else if (condition == StorageCondition.REFRIGERATED) {expireDate = 4; break;}
                else if (condition == StorageCondition.FROZEN) {break;}
                break;
            case ETC: if (condition == StorageCondition.NORMAL) {expireDate = 5; break;}
            else if (condition == StorageCondition.REFRIGERATED) {expireDate = 10; break;}
            else if (condition == StorageCondition.FROZEN) {expireDate = 60; break;}
                break;
            default: break;


        }
        return expireDate;
    }



    public List<List<String>> detectIngredient(MultipartFile file) throws IOException, InterruptedException {


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


        List<List<String>> response = sendtoOCRApi(jpgFile);

        heicFile.delete();
        jpgFile.delete();

        return response;
    }

    public List<List<String>> sendtoOCRApi(File jpgFile) {
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
            String responseString = response.toString();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseString);

            List<List<String>> lines = new ArrayList<>();
            List<String> currentLine = new ArrayList<>();

            JsonNode fields = root.get("images").get(0).get("fields");

            for (JsonNode field : fields) {
                String inferText = field.path("inferText").asText();
                boolean lineBreak = field.path("lineBreak").asBoolean();

                currentLine.add(inferText);
                if (lineBreak) {
                    lines.add(currentLine);
                    currentLine = new ArrayList<>();
                }

            }

            if (!currentLine. isEmpty()) {
                lines.add(currentLine);
            }



            System.out.println("d");
            return lines;

        } catch (Exception e) {
            throw new RuntimeException("이미지 변환 실패");
        }

    }


    public void writeMultiPart(OutputStream out, String jsonMessage, File file, String boundary) throws
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
