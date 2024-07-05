package org.kiwiproject.dropwizard.jakarta.xml.ws.example.db;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.kiwiproject.dropwizard.jakarta.xml.ws.example.core.Person;

import java.util.List;
import java.util.Optional;

/**
 * See dropwizard-example: com.example.helloworld.db.PersonDAO
 */
public class PersonDAO extends AbstractDAO<Person> {

    public PersonDAO(SessionFactory sessionFactory) {
        super(sessionFactory);

        // set up embedded database

        var session = sessionFactory.openSession();
        try {
            session.beginTransaction();
            session.createNativeQuery("create table people(id bigint primary key auto_increment not null, " +
                    "fullname varchar(256) not null, jobtitle varchar(256) not null);", Void.class).executeUpdate();
            session.createNativeQuery("create sequence hibernate_sequence", Void.class).executeUpdate();
        } finally {
            session.getTransaction().commit();
            session.close();
        }
    }

    public Optional<Person> findById(Long id) {
        return Optional.ofNullable(get(id));
    }

    public Person create(Person person) {
        return persist(person);
    }

    public List<Person> findAll() {
        @SuppressWarnings("unchecked")
        var query =
                (Query<Person>) namedQuery("org.kiwiproject.dropwizard.jakarta.xml.ws.example.core.Person.findAll");
        return list(query);
    }
}
