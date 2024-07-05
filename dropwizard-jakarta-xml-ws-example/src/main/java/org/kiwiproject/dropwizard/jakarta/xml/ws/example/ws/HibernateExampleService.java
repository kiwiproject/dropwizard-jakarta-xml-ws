package org.kiwiproject.dropwizard.jakarta.xml.ws.example.ws;

import io.dropwizard.hibernate.UnitOfWork;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.validation.Valid;
import org.kiwiproject.dropwizard.jakarta.xml.ws.example.core.Person;
import org.kiwiproject.dropwizard.jakarta.xml.ws.example.db.PersonDAO;

import java.util.List;

@WebService
public class HibernateExampleService {

    PersonDAO personDAO;

    public HibernateExampleService() {
    }

    public HibernateExampleService(PersonDAO personDAO) {
        this.personDAO = personDAO;
    }

    @WebMethod
    @UnitOfWork
    public List<Person> getPersons() {
        return this.personDAO.findAll();
    }

    @WebMethod
    @UnitOfWork
    public Person getPerson(long id) throws Exception {
        var personOptional = this.personDAO.findById(id);
        if (personOptional.isPresent()) {
            return personOptional.get();
        }

        throw new Exception("Person with id " + id + " not found");
    }

    @WebMethod
    @UnitOfWork
    public Person createPerson(@Valid Person person) {
        return this.personDAO.create(person);
    }

}
