/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.exception;

/**
 *
 * @author w2151373
 */

public class LinkedResourceNotFoundException extends RuntimeException {

    private final String fieldName;
    private final String invalidValue;

    public LinkedResourceNotFoundException(String fieldName, String invalidValue) {
        super("Invalid reference: " + fieldName + " = " + invalidValue);
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getInvalidValue() {
        return invalidValue;
    }
}

