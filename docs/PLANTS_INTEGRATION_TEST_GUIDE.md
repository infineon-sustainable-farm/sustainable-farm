# Plants Integration Testing Guide

**Purpose:** Test and verify the complete Plants module integration
**Components:** Backend API, Frontend UI, Integration Points
**Date:** 2026-09-21

---

## Prerequisites

1. **PostgreSQL database running** on localhost:5432
2. **Maven installed** for backend
3. **Node.js and npm installed** for frontend
4. **Java 17** installed

---

## Phase 1: Backend Testing

### Step 1: Start the Backend Server

```bash
cd /media/SABF_HDD/0maiN/01_My_Document/00_BIT_Journey/Bit x Infineon Excellence Program/13_Prod_Trans_App/backend
mvn spring-boot:run
```

**Expected Output:**
- Server starts on port 8080
- Logs show "Started ProductTransformationApplication"
- No errors in startup

### Step 2: Test Backend Compilation

```bash
cd /media/SABF_HDD/0maiN/01_My_Document/00_BIT_Journey/Bit x Infineon Excellence Program/13_Prod_Trans_App/backend
mvn clean compile
```

**Expected:** BUILD SUCCESS

### Step 3: Run Backend Tests

```bash
cd /media/SABF_HDD/0maiN/01_My_Document/00_BIT_Journey/Bit x Infineon Excellence Program/13_Prod_Trans_App/backend
mvn test
```

**Expected:** 67 tests passing, 0 failures, 0 errors

### Step 4: Test Plants Integration Endpoints

Once the backend is running, test the Plants integration endpoints:

#### 4.1 Get All Varieties
```bash
curl http://localhost:8080/api/integration/plants/varieties
```

**Expected:** JSON array with variety data (or empty array if Plants API not available)

#### 4.2 Get Varieties by Farm
```bash
curl http://localhost:8080/api/integration/plants/varieties/farm/FARM-001
```

**Expected:** JSON array filtered by farm

#### 4.3 Get Varieties by Parcel
```bash
curl http://localhost:8080/api/integration/plants/varieties/parcel/A
```

**Expected:** JSON array filtered by parcel

#### 4.4 Get Growth Calendar
```bash
curl "http://localhost:8080/api/integration/plants/growth-calendar?blocParcelle=A&idFerme=FARM-001"
```

**Expected:** JSON array with growth calendar data

#### 4.5 Get Growth Calendar by Farm and Parcel
```bash
curl http://localhost:8080/api/integration/plants/growth-calendar/farm/FARM-001/parcel/A
```

**Expected:** JSON array with specific growth calendar data

### Step 5: Check Swagger Documentation

Open in browser:
```
http://localhost:8080/swagger-ui.html
```

**Expected:**
- See "Plants Integration" section
- See all 5 Plants endpoints documented
- Can try endpoints from Swagger UI

---

## Phase 2: Frontend Testing

### Step 1: Start the Frontend Server

```bash
cd /media/SABF_HDD/0maiN/01_My_Document/00_BIT_Journey/Bit x Infineon Excellence Program/13_Prod_Trans_App/frontend
npm start
```

**Expected:**
- React development server starts
- Opens browser at http://localhost:3000
- No compilation errors

### Step 2: Test Frontend Build

```bash
cd /media/SABF_HDD/0maiN/01_My_Document/00_BIT_Journey/Bit x Infineon Excellence Program/13_Prod_Trans_App/frontend
npm run build
```

**Expected:** 
- Compiled successfully
- File sizes shown (should be ~200.98 kB)
- No warnings or errors

### Step 3: Test Plants Integration in UI

#### 3.1 Dashboard Page
1. Navigate to http://localhost:3000
2. Scroll down to "Plants Information" section
3. **Expected:** See varieties displayed from Plants module
4. If Plants API is not available, should show "No Plants data available" message

#### 3.2 Batch Detail Page
1. Navigate to http://localhost:3000/batches
2. Click on any batch
3. Scroll to "Plants Information" section
4. **Expected:** See varieties and growth calendar for that batch's farm and block
5. If batch has farmId and blockId, should show filtered Plants data

---

## Phase 3: Integration Testing

### Step 1: Test with Mock Plants API

Since the actual Plants API may not be running, the integration is designed to gracefully handle this:

1. **Backend behavior when Plants API unavailable:**
   - Returns empty array
   - Logs warning message
   - Does not crash

2. **Frontend behavior when Plants API unavailable:**
   - Shows "No Plants data available" message
   - No error messages to user
   - UI remains functional

### Step 2: Test Integration Configuration

Check that integration settings are correct:

```bash
cat /media/SABF_HDD/0maiN/01_My_Document/00_BIT_Journey/Bit x Infineon Excellence Program/13_Prod_Trans_App/backend/src/main/resources/application.properties
```

**Expected to see:**
```properties
integration.plants.base-url=http://localhost:8081
integration.plants.enabled=true
```

### Step 3: Test Integration Disable

To test graceful degradation:

1. Edit `application.properties`:
```properties
integration.plants.enabled=false
```

2. Restart backend
3. Test endpoints - should return empty arrays with log warning

---

## Phase 4: Code Quality Checks

### Step 1: Check Backend Code

```bash
cd /media/SABF_HDD/0maiN/01_My_Document/00_BIT_Journey/Bit x Infineon Excellence Program/13_Prod_Trans_App/backend
find src/main/java/com/sustainablefarm/modules/producttransformation/integration/plants -name "*.java"
```

**Expected files:**
- VarietyResponse.java
- GrowthCalendarResponse.java
- PlantsIntegrationService.java
- PlantsIntegrationServiceImpl.java
- PlantsIntegrationController.java

### Step 2: Check Frontend Code

```bash
cd /media/SABF_HDD/0maiN/01_My_Document/00_BIT_Journey/Bit x Infineon Excellence Program/13_Prod_Trans_App/frontend
find src -name "*plants*" -o -name "*Plants*"
```

**Expected files:**
- src/services/plantsService.js
- src/components/PlantsInfo.js
- src/components/PlantsInfo.css

### Step 3: Check Integration Points

```bash
cd /media/SABF_HDD/0maiN/01_My_Document/00_BIT_Journey/Bit x Infineon Excellence Program/13_Prod_Trans_App/frontend
grep -r "PlantsInfo" src/pages/
```

**Expected:**
- PlantsInfo imported in DashboardPage.js
- PlantsInfo imported in BatchDetailPage.js

---

## Phase 5: Data Flow Verification

### Step 1: Verify Data Mapping

Check that the backend correctly maps Plants API fields:

**Plants API field → Product Transformation field:**
- `id_ferme` → `farmId`
- `bloc_parcelle` → `parcelId`
- `nom` → `varietyName`
- `nombre_arbres` → `treeCount`
- etc.

### Step 2: Verify Parcel Identifier Normalization

Test that parcel identifiers are correctly normalized:
- Input: " A " → Output: "A"
- Input: "Bloc A" → Output: "A" (if logic exists)
- Input: "A" → Output: "A"

### Step 3: Verify Null Handling

Test that null values are handled correctly:
- `null` should mean "data unavailable"
- `0` should mean "actual zero value"
- Distinction should be preserved

---

## Troubleshooting

### Issue: Backend won't start
**Solution:** Check PostgreSQL is running, check database credentials in application.properties

### Issue: Plants endpoints return 404
**Solution:** Verify controller is registered, check package scanning configuration

### Issue: Frontend can't connect to backend
**Solution:** Check backend is running on port 8080, check CORS configuration

### Issue: Plants API connection refused
**Solution:** This is expected if Plants API is not running - integration should gracefully degrade

### Issue: Tests failing
**Solution:** Run `mvn clean test` to ensure clean state, check all dependencies are installed

---

## Success Criteria

### Backend
- ✅ Server starts without errors
- ✅ All 67 tests pass
- ✅ Plants integration endpoints respond (even with empty data)
- ✅ Swagger documentation available
- ✅ Graceful degradation when Plants API unavailable

### Frontend
- ✅ Development server starts without errors
- ✅ Production build succeeds
- ✅ PlantsInfo component renders
- ✅ Dashboard shows Plants section
- ✅ Batch Detail shows context-aware Plants data
- ✅ No console errors

### Integration
- ✅ Data mapping correct
- ✅ Parcel identifier normalization works
- ✅ Null handling correct
- ✅ Configuration works (enable/disable)
- ✅ Error handling graceful

---

## Quick Test Script

Run this quick test to verify everything is working:

```bash
#!/bin/bash

echo "=== Plants Integration Quick Test ==="

# Test 1: Backend compilation
echo "Test 1: Backend compilation..."
cd /media/SABF_HDD/0maiN/01_My_Document/00_BIT_Journey/Bit\ x\ Infineon\ Excellence\ Program/13_Prod_Trans_App/backend
mvn clean compile
if [ $? -eq 0 ]; then
    echo "✅ Backend compilation successful"
else
    echo "❌ Backend compilation failed"
    exit 1
fi

# Test 2: Backend tests
echo "Test 2: Backend tests..."
mvn test
if [ $? -eq 0 ]; then
    echo "✅ Backend tests successful"
else
    echo "❌ Backend tests failed"
    exit 1
fi

# Test 3: Frontend build
echo "Test 3: Frontend build..."
cd /media/SABF_HDD/0maiN/01_My_Document/00_BIT_Journey/Bit\ x\ Infineon\ Excellence\ Program/13_Prod_Trans_App/frontend
npm run build
if [ $? -eq 0 ]; then
    echo "✅ Frontend build successful"
else
    echo "❌ Frontend build failed"
    exit 1
fi

echo "=== All Quick Tests Passed ==="
```

---

**Last Updated:** 2026-09-21
**Status:** Ready for testing
