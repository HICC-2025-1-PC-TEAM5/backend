package hicc_project.RottenToday.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hicc_project.RottenToday.dto.*;
import hicc_project.RottenToday.entity.*;
import hicc_project.RottenToday.exception.NoInputException;
import hicc_project.RottenToday.repository.IngredientRepository;
import hicc_project.RottenToday.repository.MemberRepository;
import hicc_project.RottenToday.repository.RefrigeratorIngredientRepository;
import hicc_project.RottenToday.service.recipe.RecipeIngredientParser;
import jakarta.persistence.EntityNotFoundException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Function;

@Service
public class IngredientService {

    RefrigeratorIngredientRepository refrigeratorIngredientRepository;
    IngredientRepository ingredientRepository;
    MemberRepository memberRepository;
    RecipeIngredientParser ingredientParser; // 냉장고 재료 이름을 마스터 이름으로 맞춘다 (B23)

    @Value("${clova.ocr.url}")
    private String clovaOcrUrl;

    @Value("${clova.ocr.secret}")
    private String clovaOcrSecret;

    @Autowired
    public IngredientService(RefrigeratorIngredientRepository refrigeratorIngredientRepository, IngredientRepository ingredientRepository, MemberRepository memberRepository, RecipeIngredientParser ingredientParser) {
        this.refrigeratorIngredientRepository = refrigeratorIngredientRepository;
        this.ingredientRepository = ingredientRepository;
        this.memberRepository = memberRepository;
        this.ingredientParser = ingredientParser;
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
            // 이름은 사용자가 쓴 그대로 두고, 마스터 연결과 카테고리에만 정규화한 이름을 쓴다 (D-029)
            findMaster(dto.getName()).ifPresent(ingredient -> {
                refrigeratorIngredient.setIngredient(ingredient);
                refrigeratorIngredient.setCategory(ingredient.getCategory());
            });
            refrigeratorIngredient.setMember(member);
            LocalDateTime now = LocalDateTime.now();
            refrigeratorIngredient.setInput_date(now);
            // 사용자가 소비기한을 입력했으면 그 값을 쓰고, 없을 때만 계산한다 (D-028)
            if (refrigeratorIngredient.getExpire_date() == null) {
                int plusDays = measureExpireDate(refrigeratorIngredient.getCategory(), refrigeratorIngredient.getType());
                refrigeratorIngredient.setExpire_date(refrigeratorIngredient.getInput_date().plusDays(plusDays));
            }

            refrigeratorIngredientRepository.save(refrigeratorIngredient);
        }
    }
    @Transactional
    public void updateRefridgeIngredient(Long memberId, RefridgeIngredientRequest request) {
        Optional<RefrigeratorIngredient> byId = refrigeratorIngredientRepository.findById(request.getRefrigeratorIngredientId())
                .filter(r -> isOwnedBy(r, memberId)); // 남의 재료는 없는 것으로 취급 (D-012)
        if (byId.isPresent()) {
            RefrigeratorIngredient refrigeratorIngredient = byId.get();
            if (request.getQuantity() == 0){
                refrigeratorIngredientRepository.delete(refrigeratorIngredient);
            } else {
                refrigeratorIngredient.setQuantity(request.getQuantity());
            }
        } else {
            throw new EntityNotFoundException("해당하는 재료를 찾을 수 없음");
        }
//        List<RefrigeratorIngredient> findIngredients = refrigeratorIngredientRepository.findByMemberId(memberId);
//        for (RefrigeratorIngredient refrigeratorIngredient : findIngredients) {
//            if (refrigeratorIngredient.getId().equals(request.getRefrigeratorIngredientId())) {
//                if (request.getQuantity() == 0) {
//                    refrigeratorIngredientRepository.delete(refrigeratorIngredient);
//                } else {
//                    refrigeratorIngredient.setQuantity(request.getQuantity());
//                }
//            }
//        }
    }

    public void deleteIngredient(Long memberId, Long refridgeId) {
        // 본인 냉장고에 없는 id(남의 재료 포함)는 404 (D-012)
        RefrigeratorIngredient target = refrigeratorIngredientRepository.findByMemberId(memberId).stream()
                .filter(r -> r.getId().equals(refridgeId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("해당 냉장고 재료 없음"));
        refrigeratorIngredientRepository.delete(target);
    }

    // 마스터와 이름이 같으면 그대로, 다르면 레시피와 같은 규칙(수식어 제거 + 동의어 사전)으로 다시 찾는다. 예: 계란 → 달걀 (D-029)
    private Optional<Ingredient> findMaster(String name) {
        Optional<Ingredient> exact = ingredientRepository.findByName(name);
        if (exact.isPresent()) {
            return exact;
        }
        String normalized = ingredientParser.normalize(name);
        if (normalized.isEmpty() || normalized.equals(name)) {
            return Optional.empty();
        }
        return ingredientRepository.findByName(normalized);
    }

    private static boolean isOwnedBy(RefrigeratorIngredient item, Long memberId) {
        return item.getMember() != null && item.getMember().getId().equals(memberId);
    }

    // 반입일에 더할 일수 (D-028). 실온 육류·어패류·유제품은 당일(0일)로 둔다 — 바로 냉장·냉동하거나 오늘 쓰라는 뜻
    // 가공식품, 두류·기름·조미료의 냉장·냉동, 음료 냉동은 B24에서 채운 근삿값이다. 두류 냉장은 두부에 맞춰 짧게 잡았다
    static int measureExpireDate(Category category, StorageCondition condition) {
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
                else if (condition == StorageCondition.REFRIGERATED) {expireDate = 5; break;}
                else if (condition == StorageCondition.FROZEN) {expireDate = 90; break;}
                break;
            case OIL: if (condition == StorageCondition.NORMAL) {expireDate = 270; break;}
                else if (condition == StorageCondition.REFRIGERATED) {expireDate = 270; break;}
                else if (condition == StorageCondition.FROZEN) {expireDate = 270; break;}
                break;
            case CONDIMENT: if (condition == StorageCondition.NORMAL) {expireDate = 1000; break;}
                else if (condition == StorageCondition.REFRIGERATED) {expireDate = 365; break;}
                else if (condition == StorageCondition.FROZEN) {expireDate = 365; break;}
                break;
            case PROCESSED: if (condition == StorageCondition.NORMAL) {expireDate = 30; break;}
                else if (condition == StorageCondition.REFRIGERATED) {expireDate = 7; break;}
                else if (condition == StorageCondition.FROZEN) {expireDate = 60; break;}
                break;
            case DRINK: if (condition == StorageCondition.NORMAL) {expireDate = 135; break;}
                else if (condition == StorageCondition.REFRIGERATED) {expireDate = 4; break;}
                else if (condition == StorageCondition.FROZEN) {expireDate = 30; break;}
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

        if (file.isEmpty()){
            throw new NoInputException("입력값이 들어오지 않았습니다.");
        }

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
        String apiURL = clovaOcrUrl;
        String secretKey = clovaOcrSecret;


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


    public IngredientListResponse getIngredientList(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저는 존재하지 않습니다."));

        List<Allergy> allergies = member.getAllergies();
        List<Ingredient> ingredientList = ingredientRepository.findAll();
        List<IngredientResponse> ingredientResponseList = new ArrayList<>();

        for (Ingredient ingredient : ingredientList) {
            boolean isAllergy = false;

            for (Allergy allergy : allergies) {
                if (allergy.getIngredient().equals(ingredient)) {
                    isAllergy = true;
                    break;
                }
            }

            IngredientResponse ingredientResponse = new IngredientResponse(ingredient);
            ingredientResponse.setAllergy(isAllergy);
            ingredientResponseList.add(ingredientResponse);
        }

        return new IngredientListResponse(ingredientResponseList);
    }

    public IngredientResponseDto getIngredientDetail(Long memberId, Long ingredientId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId).orElseThrow(() -> new EntityNotFoundException("해당하는 재료 정보 없음"));
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new EntityNotFoundException("해당되는 유저 정보 없음"));
        List<Allergy> allergies = member.getAllergies();
        IngredientResponse ingredientResponse = new IngredientResponse(ingredient);
        for (Allergy allergy : allergies) { //재료 정보에 알러지 표시
            if (allergy.getIngredient().equals(ingredient)) {
                ingredientResponse.setAllergy(true);
                break;
            } else {
                ingredientResponse.setAllergy(false);
            }
        }
        return new IngredientResponseDto(ingredientResponse);

    }

    public IngredientListResponse getNecessaryIngredients(Long memberId) { //기본적인 재료 구비 안되어있을때 구비 추천 기능
        List<String> ingredient = new ArrayList<>(
                Arrays.asList("간장", "소금", "된장", "고추장", "식용유", "마늘", "쌀")
        );
        List<IngredientResponse> ingredientResponseList = new ArrayList<>();
        List<RefrigeratorIngredient> byMemberId = refrigeratorIngredientRepository.findByMemberId(memberId);
        for (RefrigeratorIngredient refrigeratorIngredient : byMemberId) {
            if (ingredient.contains(refrigeratorIngredient.getName())){
                ingredient.remove(refrigeratorIngredient.getName());
            }
        }
        // 이름마다 조회하지 않고 한 번에 조회한다. 순서는 위 목록 순서, 마스터에 없는 이름은 예전처럼 예외
        Map<String, Ingredient> masters = ingredientRepository.findAllByNameIn(ingredient).stream()
                .collect(Collectors.toMap(Ingredient::getName, Function.identity(), (x, y) -> x));
        for (String ingredientName : ingredient) {
            Ingredient byName = Optional.ofNullable(masters.get(ingredientName)).orElseThrow();
            IngredientResponse ingredientResponse = new IngredientResponse(byName);
            ingredientResponseList.add(ingredientResponse);
        }

        return new IngredientListResponse(ingredientResponseList);

    }

    public RefridgeDto getRefridgeIngredient(Long memberId, Long refrigeratorId) {
        Optional<RefrigeratorIngredient> byId = refrigeratorIngredientRepository.findById(refrigeratorId)
                .filter(r -> isOwnedBy(r, memberId));
        if (byId.isPresent()) {
            RefrigeratorIngredient refrigeratorIngredient = byId.get();
            return new RefridgeDto(refrigeratorIngredient);
        } else {
            throw new EntityNotFoundException("해당 냉장고 재료 없음");
        }
    }
}
