package com.ll.spirits.review;

import com.ll.spirits.product.Product;
import com.ll.spirits.product.ProductService;
import com.ll.spirits.user.SiteUser;
import com.ll.spirits.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
@RequestMapping("/review")
@RequiredArgsConstructor // 변수를 포함하는 생성자를 자동으로 생성.
@Controller
public class ReviewController {
    private final ProductService productService;
    private final UserService userService;
    private final ReviewService reviewService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create/{id}")
    public String createReview(Model model, @PathVariable("productId") Integer id, @Valid ReviewForm reviewForm, BindingResult bindingResult, Principal principal) {
        Product product = this.productService.getProduct(id);
        // TODO: 리뷰를 저장한다.
        SiteUser siteUser = this.userService.getUser(principal.getName());
        if (bindingResult.hasErrors()) {
            model.addAttribute("product", product);
            return "product_detail";
        }
        Review review = this.reviewService.create(product,
                reviewForm.getFlavor(), reviewForm.getAroma(), reviewForm.getContent(), siteUser);
        return String.format("redirect:/product/detail/%s#review_%s", review.getProduct().getId(), review.getId());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/modify/{id}")
    public String reviewModify(ReviewForm reviewForm, @PathVariable("id") Long id, Principal principal) {
        Review review = this.reviewService.getReview(id);
        if (!review.getAuthor().getUserId().equals(principal.getName())) { // getName 부분 뭐랑 연결되어있는지 잘 이해안됨
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
        }
        reviewForm.setFlavor(review.getFlavor());
        reviewForm.setAroma(review.getAroma());
        reviewForm.setContent(review.getContent());
        return "review_form";
    }
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/modify/{id}")
    public String reviewModify(@Valid ReviewForm reviewForm, @PathVariable("id") Long id,
                               BindingResult bindingResult, Principal principal) {
        if (bindingResult.hasErrors()) {
            return "review_form";
        }
        Review review = this.reviewService.getReview(id);
        if (!review.getAuthor().getUserId().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
        }
        this.reviewService.modify(review, reviewForm.getFlavor(), reviewForm.getAroma(), reviewForm.getContent());
        return String.format("redirect:/product/detail/%s#review_%s",
                review.getProduct().getId(), review.getId());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/delete/{id}")
    public String reviewDelete(Principal principal, @PathVariable("id") Long id) {
        Review review = this.reviewService.getReview(id);
        if (!review.getAuthor().getUserId().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제권한이 없습니다.");
        }
        this.reviewService.delete(review);
        return String.format("redirect:/product/detail/%s", review.getProduct().getId());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/vote/{id}")
    public String reviewVote(Principal principal, @PathVariable("id") Long id) {
        Review review = this.reviewService.getReview(id);
        SiteUser siteUser = this.userService.getUser(principal.getName());
        this.reviewService.vote(review, siteUser);
        return String.format("redirect:/product/detail/%s#review_%s",
                review.getProduct().getId(), review.getId());
    }

}