package ru.itis.test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.itis.orm.management.api.EntityManager;
import ru.itis.orm.management.api.EntityManagerFactory;
import ru.itis.test.model.Passport;
import ru.itis.test.model.Post;
import ru.itis.test.model.User;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
        EntityManagerFactory entityManagerFactory = context.getBean(EntityManagerFactory.class);
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Passport passport = new Passport();
        User user = User.builder()
                .name("TEST1")
                .passport(passport)
                .build();
        Post post1 = Post.builder().user(user).build();
        Post post2 = Post.builder().user(user).build();
        user.setPosts(List.of(post1, post2));
        entityManager.save(user);
        Passport findedPassport = entityManager.find(Passport.class, 1);
        User findedUser = entityManager.find(User.class, 1);
        Post findedPost = entityManager.find(Post.class, 1);
        entityManager.remove(user);

        user.setName("TEST_UPDATED");
        User updated = entityManager.update(user);
        List<Post> all = entityManager.findAll(Post.class);
        System.out.println();

    }

}
