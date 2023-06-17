package com.ll.spirits.product;

import com.ll.spirits.DataNotFoundException;
import com.ll.spirits.product.productEntity.abvRange.ABVrange;
import com.ll.spirits.product.productEntity.abvRange.ABVrangeRepository;
import com.ll.spirits.product.productEntity.cask.Cask;
import com.ll.spirits.product.productEntity.cask.CaskRepository;
import com.ll.spirits.product.productEntity.costRange.CostRange;
import com.ll.spirits.product.productEntity.costRange.CostRangeRepository;
import com.ll.spirits.product.productEntity.mainCategory.MainCategory;
import com.ll.spirits.product.productEntity.mainCategory.MainCategoryRepository;
import com.ll.spirits.product.productEntity.nation.Nation;
import com.ll.spirits.product.productEntity.nation.NationRepository;
import com.ll.spirits.product.productEntity.netWeight.NetWeight;
import com.ll.spirits.product.productEntity.netWeight.NetWeightRepository;
import com.ll.spirits.product.productEntity.pairing.Pairing;
import com.ll.spirits.product.productEntity.pairing.PairingRepository;
import com.ll.spirits.product.productEntity.subCategory.SubCategory;
import com.ll.spirits.product.productEntity.subCategory.SubCategoryRepository;
import com.ll.spirits.review.Review;
import com.ll.spirits.review.ReviewRepository;
import com.ll.spirits.user.SiteUser;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ProductService {
    private final ProductForm productForm;
    private final ProductRepository productRepository;
    // 이하 외래키로 들어오는 것들.
    private final ReviewRepository reviewRepository;
    private final MainCategoryRepository mainCategoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final CostRangeRepository costRangeRepository;
    private final ABVrangeRepository abvRangeRepository;
    private final NetWeightRepository netWeightRepository;
    private final NationRepository nationRepository;
    private final CaskRepository caskRepository;
    private final PairingRepository pairingRepository;

    private Specification<Product> search(String kw) {
        return new Specification<>() {
            private static final long serialVersionUID = 1L;
            @Override
            public Predicate toPredicate(Root<Product> p, CriteriaQuery<?> query, CriteriaBuilder cb) {
                query.distinct(true);  // 중복을 제거
                Join<Product, Review> r = p.join("reviewList", JoinType.LEFT);
                Join<Review, SiteUser> u = r.join("author", JoinType.LEFT);
                return cb.or(cb.like(p.get("name"), "%" + kw + "%"), // 술이름
                        cb.like(p.get("content"), "%" + kw + "%"),      // 내용
                        cb.like(r.get("content"), "%" + kw + "%"),      // 리뷰 내용
                        cb.like(u.get("username"), "%" + kw + "%"));   // 리뷰 작성자
            }
        };
    }

    public List<Product> getList() {
        return this.productRepository.findAll();
    }

    public Product getProduct(Integer id) {
        Optional<Product> product = this.productRepository.findById(id);
        if (product.isPresent()){
            return product.get();
        } else {
            throw new DataNotFoundException("product not found"); // 예외처리로 에러(DataNotFoundException)를 표시
        }
    }
    public List<Product> getProductsByMainCategoryId(Integer mainCategory) {
        // ProductRepository를 사용하여 mainCategory에 해당하는 제품 리스트를 조회합니다.
        return productRepository.findByMainCategoryId(mainCategory);
    }
    public List<Product> getProductsBySubCategoryId(Integer subCategoryId) {
        return productRepository.findBySubCategoryId(subCategoryId);
    }

    public List<Product> getProductsByMainCategoryIdAndSubCategoryId(Integer mainCategoryId, Integer subCategoryId) {
        // ProductRepository를 사용하여 mainCategory에 해당하는 제품 리스트를 조회합니다.
        return productRepository.getProductsByMainCategoryIdAndSubCategoryId(mainCategoryId, subCategoryId);
    }

    public List<Product> getProductsByCategory(String mainCategoryId, String subCategoryId) { // 메인카테고리와 서브카테고리 같이 찾는 로직
        return this.productRepository.getProductsByMainCategoryAndSubCategory(mainCategoryId, subCategoryId);
    }

    public List<Review> getReviewsByProduct(Product product) { // 리뷰부분 제대로 작동하지 않을 시 최우선으로 삭제 고려할 것
        return reviewRepository.findByProduct(product);
    }

    public void createProduct(ProductForm productForm, SiteUser siteUser) {
        MainCategory mainCategory = mainCategoryRepository.findById(productForm.getMainCategoryId())
                .orElseThrow(() -> new DataNotFoundException("mainCategory not found"));

        SubCategory subCategory = subCategoryRepository.findById(productForm.getSubCategoryId())
                .orElseThrow(() -> new DataNotFoundException("subCategory not found"));

        CostRange costRange = costRangeRepository.findById(productForm.getCostRangeId())
                .orElseThrow(() -> new DataNotFoundException("costRange not found"));

        ABVrange abvRange = abvRangeRepository.findById(productForm.getAbvRangeId())
                .orElseThrow(() -> new DataNotFoundException("abvRange not found"));

        NetWeight netWeight = netWeightRepository.findById(productForm.getNetWeightId())
                .orElseThrow(() -> new DataNotFoundException("netWeight not found"));

        Nation nation = nationRepository.findById(productForm.getNationId())
                .orElseThrow(() -> new DataNotFoundException("nation not found"));

        // 캐스크 ID 리스트 검증 및 조회
        List<Integer> caskList = productForm.getCasks();
        if (caskList == null || caskList.isEmpty() || caskList.contains(null)) {
            // caskList가 null이거나 비어 있거나 null 값을 포함하고 있는 경우,
            // caskList를 null을 단일 요소로 갖는 리스트로 초기화합니다.
            caskList = Collections.singletonList(null);
        } else {
            // caskList를 처리하여 빈 문자열을 제거하고 null로 변환합니다.
            // "none"인 경우에도 null로 변환합니다.
            caskList = caskList.stream()
                    .filter(casks -> casks != null && !casks.equals("none") && casks.longValue() > 0)
                    .map(casks -> casks.equals("") ? null : casks)
                    .collect(Collectors.toList());
        }

        // caskList에 해당하는 캐스크 객체들을 caskRepository에서 조회합니다.
        List<Cask> casks = caskRepository.findAllById(caskList);
//        if (casks.size() != caskList.size()) {
//            // 조회된 casks의 크기와 caskList의 크기가 일치하지 않는 경우,
//            // DataNotFoundException을 발생시킵니다.
//            throw new DataNotFoundException("존재하지 않는 캐스크 ID가 포함되어 있습니다.");
//        }

        // 페어링 ID 리스트 검증 및 조회
        List<Integer> pairingList = productForm.getPairings();
        if (pairingList == null || pairingList.isEmpty()) {
            throw new IllegalArgumentException("어울리는 안주는 필수 입력항목입니다.");
        }

        List<Pairing> pairings = pairingRepository.findAllById(pairingList);
        if (pairings.size() != pairingList.size()) {
            throw new DataNotFoundException("존재하지 않는 페어링 ID가 포함되어 있습니다.");
        }

        Product product = new Product();
        product.setName(productForm.getName());
        product.setAbv(productForm.getAbv());
        product.setAroma(productForm.getAroma());
        product.setFlavor(productForm.getFlavor());
        product.setInfo(productForm.getInfo());
        product.setCost(productForm.getCost());
        product.setMainCategory(mainCategory);
        product.setSubCategory(subCategory);
        product.setCostRange(costRange);
        product.setAbvRange(abvRange);
        product.setNetWeight(netWeight);
        product.setNation(nation);
        product.setAuthor(siteUser);
        // product를 먼저 저장합니다.
        product = productRepository.save(product);
        // product와 맵핑되는 pairings를 생성합니다.
        for (Pairing pairing : pairings) {
            product.getPairings().add(pairing);
        }
        // product와 맵핑되는 casks를 생성합니다.
        for (Cask cask : casks) {
            product.getCasks().add(cask);
        }
        // 저장된 product를 다시 저장합니다.
        productRepository.save(product);
    }




    public void vote(Product product, SiteUser siteUser) { // 추천 메서드
        product.getVoter().add(siteUser);
        this.productRepository.save(product);
    }

    public void cancelVote(Product product, SiteUser siteUser) { // 추천 취소 메서드
        product.getVoter().remove(siteUser);
        this.productRepository.save(product);
    }

    public void wish(Product product, SiteUser siteUser) { // 찜 메서드
        product.getVoter().add(siteUser);
        this.productRepository.save(product);
    }

    public void cancleWish(Product product, SiteUser siteUser) { // 찜 취소 메서드
        product.getVoter().remove(siteUser);
        this.productRepository.save(product);
    }

    public void modify(Integer id, ProductForm productForm, SiteUser siteUser) { // 추천 메서드
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            // 제품이 존재하지 않을 경우, 에러 처리 또는 다른 동작 수행
            System.out.println("존재하지 않는 제품입니다.");
            return;
        }

        MainCategory mainCategory = mainCategoryRepository.findById(productForm.getMainCategoryId())
                .orElseThrow(() -> new DataNotFoundException("mainCategory not found"));

        SubCategory subCategory = subCategoryRepository.findById(productForm.getSubCategoryId())
                .orElseThrow(() -> new DataNotFoundException("subCategory not found"));

        CostRange costRange = costRangeRepository.findById(productForm.getCostRangeId())
                .orElseThrow(() -> new DataNotFoundException("costRange not found"));

        ABVrange abvRange = abvRangeRepository.findById(productForm.getAbvRangeId())
                .orElseThrow(() -> new DataNotFoundException("abvRange not found"));

        NetWeight netWeight = netWeightRepository.findById(productForm.getNetWeightId())
                .orElseThrow(() -> new DataNotFoundException("netWeight not found"));

        Nation nation = nationRepository.findById(productForm.getNationId())
                .orElseThrow(() -> new DataNotFoundException("nation not found"));

        // 캐스크 ID 리스트 검증 및 조회
        List<Integer> caskList = productForm.getCasks();
        if (caskList == null || caskList.isEmpty() || caskList.contains(null)) {
            // caskList가 null이거나 비어 있거나 null 값을 포함하고 있는 경우,
            // caskList를 null을 단일 요소로 갖는 리스트로 초기화합니다.
            caskList = Collections.singletonList(null);
        } else {
            // caskList를 처리하여 빈 문자열을 제거하고 null로 변환합니다.
            // "none"인 경우에도 null로 변환합니다.
            caskList = caskList.stream()
                    .filter(casks -> casks != null && !casks.equals("none") && casks.longValue() > 0)
                    .map(casks -> casks.equals("") ? null : casks)
                    .collect(Collectors.toList());
        }

        // caskList에 해당하는 캐스크 객체들을 caskRepository에서 조회합니다.
        List<Cask> casks = caskRepository.findAllById(caskList);
//        if (casks.size() != caskList.size()) {
//            // 조회된 casks의 크기와 caskList의 크기가 일치하지 않는 경우,
//            // DataNotFoundException을 발생시킵니다.
//            throw new DataNotFoundException("존재하지 않는 캐스크 ID가 포함되어 있습니다.");
//        }

        // 페어링 ID 리스트 검증 및 조회
        List<Integer> pairingList = productForm.getPairings();
        if (pairingList == null || pairingList.isEmpty()) {
            throw new IllegalArgumentException("어울리는 안주는 필수 입력항목입니다.");
        }

        List<Pairing> pairings = pairingRepository.findAllById(pairingList);
        if (pairings.size() != pairingList.size()) {
            throw new DataNotFoundException("존재하지 않는 페어링 ID가 포함되어 있습니다.");
        }
        // 제품 정보 업데이트
        product.setName(productForm.getName());
        product.setAbv(productForm.getAbv());
        product.setAroma(productForm.getAroma());
        product.setFlavor(productForm.getFlavor());
        product.setInfo(productForm.getInfo());
        product.setCost(productForm.getCost());
        product.setMainCategory(mainCategory);
        product.setSubCategory(subCategory);
        product.setCostRange(costRange);
        product.setAbvRange(abvRange);
        product.setNetWeight(netWeight);
        product.setNation(nation);
        product.setAuthor(siteUser);
        // product를 먼저 저장합니다.
        product = productRepository.save(product);
        // product와 맵핑되는 pairings를 생성합니다.
        for (Pairing pairing : pairings) {
            product.getPairings().add(pairing);
        }
        // product와 맵핑되는 casks를 생성합니다.
        for (Cask cask : casks) {
            product.getCasks().add(cask);
        }
        // 저장된 product를 다시 저장합니다.
        productRepository.save(product);
    }

    public void delete(Product product) { // 삭제 메서드
        this.productRepository.delete(product);
    }

}