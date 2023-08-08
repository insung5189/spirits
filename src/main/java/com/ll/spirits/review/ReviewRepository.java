package com.ll.spirits.review;

import com.ll.spirits.product.Product;
import com.ll.spirits.product.productEntity.mainCategory.MainCategory;
import com.ll.spirits.user.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProduct(Product product);
    List<Review> findById(Review review);
    List<Review> findByAuthor(SiteUser author);

}