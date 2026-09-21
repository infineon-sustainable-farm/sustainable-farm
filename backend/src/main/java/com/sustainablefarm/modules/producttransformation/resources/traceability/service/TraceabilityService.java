package com.sustainablefarm.modules.producttransformation.resources.traceability.service;

import com.sustainablefarm.modules.producttransformation.resources.traceability.dto.response.TraceabilityResponse;

/**
 * Service interface for Traceability operations
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
public interface TraceabilityService {
    
    /**
     * Get complete traceability chain for a batch
     * 
     * @param batchId The batch ID
     * @return Complete traceability response from harvest to packaging
     */
    TraceabilityResponse getBatchTraceability(String batchId);
}