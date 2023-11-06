package com.roskart.dropwizard.jaxws.example.db;

import com.google.common.base.Optional;
import com.roskart.dropwizard.jaxws.example.core.Person;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.List;

/**
 * See dropwizard-example: com.example.helloworld.db.PersonDAO
 */
public class PersonDAO extends AbstractDAO<Person> {

    public PersonDAO(SessionFactory sessionFactory) {
        super(sessionFactory);

        // set up embedded database

        Session sess = sessionFactory.openSession();
        try {
            sess.beginTransaction();
            sess.createNativeQuery("create table people(id bigint primary key auto_increment not null, " +
                    "fullname varchar(256) not null, jobtitle varchar(256) not null);", Void.class).executeUpdate();
            sess.createNativeQuery("create sequence hibernate_sequence", Void.class).executeUpdate();
        }
        finally {
            sess.getTransaction().commit();
            sess.close();
        }
    }

    public Optional<Person> findById(Long id) {
        return Optional.fromNullable(get(id));
    }

    public Person create(Person person) {
        return persist(person);
    }

    public List<Person> findAll() {
        @SuppressWarnings("unchecked")
        Query<Person> query =
                (Query<Person>) namedQuery("com.roskart.dropwizard.jaxws.example.core.Person.findAll");
        return list(query);
    }
}
