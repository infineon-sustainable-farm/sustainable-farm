package com.sustainablefarm.core.config;

import com.sustainablefarm.modules.producttransformation.resources.packagingrecord.model.PackagingRecord.PackageType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Attribute Converter for PackageType enum
 * Handles mapping between Java enum and database values
 * 
 * Database values: "1KG_BAG", "2KG_BAG", "BULK"
 * Java enum values: ONE_KG_BAG, TWO_KG_BAG, BULK
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Converter(autoApply = true)
public class PackageTypeConverter implements AttributeConverter<PackageType, String> {

    @Override
    public String convertToDatabaseColumn(PackageType packageType) {
        if (packageType == null) {
            return null;
        }
        switch (packageType) {
            case ONE_KG_BAG:
                return "1KG_BAG";
            case TWO_KG_BAG:
                return "2KG_BAG";
            case BULK:
                return "BULK";
            default:
                throw new IllegalArgumentException("Unknown PackageType: " + packageType);
        }
    }

    @Override
    public PackageType convertToEntityAttribute(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        switch (dbValue) {
            case "1KG_BAG":
                return PackageType.ONE_KG_BAG;
            case "2KG_BAG":
                return PackageType.TWO_KG_BAG;
            case "BULK":
                return PackageType.BULK;
            default:
                throw new IllegalArgumentException("Unknown database value: " + dbValue);
        }
    }
}