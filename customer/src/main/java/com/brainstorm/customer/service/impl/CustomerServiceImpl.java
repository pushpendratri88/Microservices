package com.brainstorm.customer.service.impl;

import com.brainstorm.customer.dto.CustomerDTO;
import com.brainstorm.customer.entity.Customer;
import com.brainstorm.customer.exception.CustomerAlreadyExistsException;
import com.brainstorm.customer.exception.ResourceNotFoundException;
import com.brainstorm.customer.mapper.CustomerMapper;
import com.brainstorm.customer.repository.CustomerRepository;
import com.brainstorm.customer.service.ICustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerServiceImpl implements ICustomerService {
    @Autowired
    CustomerRepository customerRepository;
    @Override
    public CustomerDTO fetchCustomerDetails(String mobileNumber) {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(() -> new ResourceNotFoundException("Customer", "MobileNumber", mobileNumber));
        return CustomerMapper.mapToCustomerDTO(customer,new CustomerDTO());
    }

    @Override
    public CustomerDTO fetchCustomerDetailsWithEmail(String mobileNumber, String email) {
        Customer customer = customerRepository.findByMobileNumberAndEmail(mobileNumber,email).orElseThrow(() -> new ResourceNotFoundException("Customer", "MobileNumber & Email ", mobileNumber +"&" +email));
        return CustomerMapper.mapToCustomerDTO(customer,new CustomerDTO());
    }

    @Override
    public void createNewCustomer(CustomerDTO customerDTO) {
        Optional<Customer> optionalCustomer = customerRepository.findByMobileNumber(customerDTO.getMobileNumber());
        if(optionalCustomer.isPresent()){
            throw new CustomerAlreadyExistsException("Customer already registered with given mobileNumber "
                    +customerDTO.getMobileNumber());
        }else {
            Customer customerEntity = CustomerMapper.mapToCustomer(customerDTO, new Customer());
            customerRepository.save(customerEntity);
        }
    }

    @Override
    public void updateCustomer(CustomerDTO customerDTO) {
        Optional<Customer> optionalCustomer =  customerRepository.findByMobileNumber(customerDTO.getMobileNumber());
        if(optionalCustomer.isEmpty()){
            throw new ResourceNotFoundException("Customer not registered", "CustomerEntity", customerDTO.getMobileNumber() );
        }
        CustomerMapper.mapToCustomer(customerDTO,optionalCustomer.get());
        customerRepository.save(optionalCustomer.get());
    }

    @Override
    public void removeCustomer(String mobileNumber) {
        Optional<Customer> optionalCustomer =  customerRepository.findByMobileNumber(mobileNumber);
        optionalCustomer.ifPresent(customer -> customerRepository.delete(customer));
    }
}
