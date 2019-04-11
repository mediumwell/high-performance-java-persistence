package com.vladmihalcea.book.hpjp.hibernate.envers;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.hibernate.equality.AbstractEqualityCheckTest;
import com.vladmihalcea.book.hpjp.hibernate.identifier.Identifiable;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.transaction.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class EnversAuditedDefaultStrategyTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Post.class
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId( 1L );
            post.setTitle( "High-Performance Java Persistence 1st edition" );
            entityManager.persist( post );
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find( Post.class, 1L );
            post.setTitle( "High-Performance Java Persistence 2nd edition" );
        });

        doInJPA(entityManager -> {
            entityManager.remove(
                entityManager.getReference( Post.class, 1L )
            );
        });

        doInJPA((JPATransactionVoidFunction)(entityManager -> {
            List<Post> posts = AuditReaderFactory.get( entityManager )
            .createQuery()
            .forRevisionsOfEntity( Post.class, true, true )
            .add( AuditEntity.id().eq( 1L ) )
            .getResultList();

            assertEquals( 3, posts.size() );

            for ( int i = 0; i < posts.size(); i++ ) {
                LOGGER.info( "Revision {} of Post entity: {}", i + 1, posts.get( i ) );
            }
        }));

        List<Number> revisions = doInJPA( entityManager -> {
            return AuditReaderFactory.get( entityManager ).getRevisions(
                Post.class, 1L
            );
        } );

        doInJPA( entityManager -> {
            Post post = (Post) AuditReaderFactory.get( entityManager )
            .createQuery()
            .forEntitiesAtRevision( Post.class, revisions.get( 0 ) )
            .getSingleResult();

            assertEquals("High-Performance Java Persistence 1st edition", post.getTitle());
        } );

    }

    @Entity(name = "Post")
    @Table(name = "post")
    @Audited
    public static class Post {

        @Id
        private Long id;

        private String title;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return "Post{" +
                    "id=" + id +
                    ", title='" + title + '\'' +
                    '}';
        }
    }
}
