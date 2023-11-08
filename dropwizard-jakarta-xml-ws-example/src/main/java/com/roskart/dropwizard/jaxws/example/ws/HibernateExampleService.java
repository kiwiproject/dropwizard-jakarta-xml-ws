package com.roskart.dropwizard.jaxws.example.ws;

import com.roskart.dropwizard.jaxws.example.core.Person;
import com.roskart.dropwizard.jaxws.example.db.PersonDAO;
import io.dropwizard.hibernate.UnitOfWork;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

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
        Optional<Person> result = this.personDAO.findById(id);
        if (result.isPresent()) {
            return result.get();
        }

        throw new Exception("Person with id " + id + " not found");
    }

    @WebMethod
    @UnitOfWork
    public Person createPerson(@Valid Person person) {
        return this.personDAO.create(person);
    }

}
