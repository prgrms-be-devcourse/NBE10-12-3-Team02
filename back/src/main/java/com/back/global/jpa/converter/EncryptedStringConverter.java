package com.back.global.jpa.converter;

import com.back.global.util.AesEncryptionUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;

@Converter
@RequiredArgsConstructor
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final AesEncryptionUtil aesEncryptionUtil;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return aesEncryptionUtil.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return aesEncryptionUtil.decrypt(dbData);
    }
}