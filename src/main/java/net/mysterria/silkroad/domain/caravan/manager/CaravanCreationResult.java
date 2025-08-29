package net.mysterria.silkroad.domain.caravan.manager;

import net.mysterria.silkroad.domain.caravan.model.Caravan;

/**
 * Result of caravan creation attempt with detailed information
 */
public class CaravanCreationResult {
    
    private final boolean success;
    private final String message;
    private final Caravan caravan;
    
    private CaravanCreationResult(boolean success, String message, Caravan caravan) {
        this.success = success;
        this.message = message;
        this.caravan = caravan;
    }
    
    public static CaravanCreationResult success(Caravan caravan) {
        return new CaravanCreationResult(true, "Caravan created successfully", caravan);
    }
    
    public static CaravanCreationResult error(String message) {
        return new CaravanCreationResult(false, message, null);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Caravan getCaravan() {
        return caravan;
    }
}