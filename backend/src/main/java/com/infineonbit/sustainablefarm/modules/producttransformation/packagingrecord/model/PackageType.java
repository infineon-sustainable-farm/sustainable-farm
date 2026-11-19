package com.infineonbit.sustainablefarm.modules.producttransformation.packagingrecord.model;

/**
 * Package Type Enum
 * Business Rule: Matches database CHECK constraint with custom converter
 */
public enum PackageType {
    ONE_KG_BAG,  // Maps to "1KG_BAG" in database via PackageTypeConverter
    TWO_KG_BAG,  // Maps to "2KG_BAG" in database via PackageTypeConverter
    BULK
}
