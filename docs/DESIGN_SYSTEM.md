# SUSTAINABLE FARM - DESIGN SYSTEM SPECIFICATION

**Program:** BIT × Infineon Excellence Program  
**Project:** Sustainable Farm - Product Transformation  
**Workstream:** Product Transformation  
**Week:** 6  
**Phase:** 2 — Design System Formalization  
**Owner:** Abdoul Ben Fatao SANON  
**Created:** 2026-09-01  
**Status:** ✅ FORMALIZED

---

## 01. EXECUTIVE SUMMARY

This document formalizes the design system extracted from the validated Week 5 mock-up. The design system establishes consistent visual rules for the Product Transformation application, addressing Week 5 stakeholder feedback regarding logo treatment, typography consistency, color usage, and chart visualization standards.

**Primary Objective:** Create a comprehensive design system that ensures consistency across all frontend components while addressing specific Week 5 feedback items.

**Outcome:** Complete design system specification with logo standards, typography hierarchy, color palette, component library, and chart visualization guidelines.

---

## 02. WEEK 5 FEEDBACK RESOLUTION

### 2.1 Feedback Mapping

| Week 5 Feedback | Resolution | Status |
|----------------|------------|--------|
| Logo must have appropriate/white background and adequate size | Documented logo specifications with white background requirement | ✅ RESOLVED |
| Typography, colors and component styles must be consistent | Formalized typography hierarchy and color usage rules | ✅ RESOLVED |
| Charts must clearly describe X/Y axes and units | Defined chart visualization standards with axis/unit requirements | ✅ RESOLVED |
| Search functionality | Confirmed preservation in mock-up, documented in component library | ✅ PRESERVED |
| Dark theme | Confirmed preservation in mock-up, documented as optional feature | ✅ PRESERVED |

### 2.2 Design System Principles

**Consistency:** All components must follow the defined color, typography, and spacing rules
**Clarity:** Chart axes and labels must clearly identify dimensions and units
**Accessibility:** High contrast ratios for text and interactive elements
**Responsiveness:** Components must adapt gracefully to different screen sizes
**Brand Alignment:** All visual elements must align with the Infineon Eucalyptus (#0a8276) brand color

---

## 03. LOGO SPECIFICATIONS

### 3.1 Logo Standards

**Primary Logo:** Infineon/Sustainable Farm logo

**Dimensions:**
- **Standard Size:** 40px × 40px (sidebar)
- **Minimum Size:** 32px × 32px (mobile)
- **Maximum Size:** 64px × 64px (hero section)

**Background Treatment:**
- **Required:** White background for all logo placements
- **Alternative:** Light gray (#F8F9FA) for hero sections
- **Forbidden:** Colored backgrounds, patterned backgrounds, transparent backgrounds on colored surfaces

**Spacing:**
- **Minimum Clear Space:** 8px (0.5rem) around logo
- **Standard Clear Space:** 16px (1rem) around logo
- **Clear Space Zone:** No text or elements within clear space

**Usage Rules:**
- Always maintain aspect ratio
- Never stretch or distort the logo
- Use approved logo variations only
- Ensure adequate contrast against background

**File Formats:**
- **SVG:** Preferred for scalability
- **PNG:** Acceptable for web use (with transparent background)
- **JPG:** Not recommended (lossy compression)

### 3.2 Logo Placement

**Sidebar Logo:**
- **Position:** Top-left of sidebar
- **Size:** 40px × 40px
- **Background:** White (#FFFFFF)
- **Padding:** 4px internal padding within white container

**Header Logo:**
- **Position:** Top-left of main content area
- **Size:** 48px × 48px
- **Background:** White (#FFFFFF)
- **Padding:** 8px internal padding

**Document Logo:**
- **Position:** Document header
- **Size:** 32px × 32px
- **Background:** White (#FFFFFF)
- **Padding:** 4px internal padding

---

## 04. TYPOGRAPHY HIERARCHY

### 4.1 Font Family

**Primary Fonts:**
- **Body Font:** Inter (Google Fonts)
  - **Fallback:** -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif
  - **Usage:** Body text, UI elements, data displays
  
- **Display Font:** Montserrat (Google Fonts)
  - **Fallback:** -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif
  - **Usage:** Headings, titles, emphasis text

- **Monospace Font:** SF Mono, JetBrains Mono, Fira Code, Consolas, monospace
  - **Usage:** Code, data values, technical information

**Font Loading:**
```html
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Montserrat:wght@500;600;700&display=swap" rel="stylesheet">
```

### 4.2 Font Scale

**Display Font (Montserrat):**
- **H1:** 1.75rem (28px) - Page titles
- **H2:** 1.5rem (24px) - Section titles
- **H3:** 1.25rem (20px) - Subsection titles
- **H4:** 1.125rem (18px) - Card titles
- **H5:** 1rem (16px) - Small headings
- **H6:** 0.875rem (14px) - Micro headings

**Body Font (Inter):**
- **Base:** 14px (0.875rem) - Body text
- **Small:** 0.875rem (14px) - Secondary text
- **Extra Small:** 0.75rem (12px) - Labels, captions
- **Large:** 1.125rem (18px) - Emphasized body text
- **Extra Large:** 1.25rem (20px) - Lead text

### 4.3 Font Weights

- **400 (Normal):** Body text, descriptions
- **500 (Medium):** Labels, form labels
- **600 (Semibold):** Headings, emphasis
- **700 (Bold):** Strong emphasis, CTAs

### 4.4 Typography Rules

**Heading Hierarchy:**
- H1 for page titles (one per page)
- H2 for major sections (2-4 per page)
- H3 for subsections (as needed)
- H4-H6 for nested content
- Never skip heading levels (e.g., H1 → H3)

**Text Colors:**
- **Primary Text:** #202124 (var(--ink))
- **Secondary Text:** #5F6368 (var(--ink-soft))
- **Muted Text:** #9AA0A6 (var(--muted))
- **Accent Text:** #0a8276 (var(--primary))
- **Success Text:** #4caf50 (var(--pass))
- **Error Text:** #c62828 (var(--fail))
- **Warning Text:** #ef6c00 (var(--warning))

**Line Height:**
- **Headings:** 1.3
- **Body Text:** 1.5
- **Compact Text:** 1.2

**Text Alignment:**
- **Default:** Left-aligned
- **Center:** For logos, centered content
- **Right:** For numeric data, timestamps
- **Justified:** Never use

---

## 05. COLOR PALETTE

### 5.1 Brand Colors

**Primary Color (Infineon Eucalyptus):**
- **Primary:** #0a8276 (var(--primary))
- **Primary Soft:** #086b61 (var(--primary-soft))
- **Primary Tint:** #E0F2F1 (var(--primary-tint))
- **Primary Hover:** #075752 (var(--primary-hover))

**Usage:**
- Primary buttons, links, active states
- Brand accents, highlights
- Logo background (white required)
- Focus states

### 5.2 Secondary Colors

**Secondary Orange:**
- **Base:** #ef6c00 (var(--secondary-orange))
- **Soft:** #cc5c00 (var(--secondary-orange-soft))
- **Tint:** #FFF3E0 (var(--secondary-orange-tint))

**Secondary Red:**
- **Base:** #c62828 (var(--secondary-red))
- **Soft:** #a02020 (var(--secondary-red-soft))
- **Tint:** #FFEBEE (var(--secondary-red-tint))

**Secondary Green:**
- **Base:** #4caf50 (var(--secondary-green))
- **Soft:** #3d8b40 (var(--secondary-green-soft))
- **Tint:** #E8F5E9 (var(--secondary-green-tint))

### 5.3 Status Colors

**Success (Pass):**
- **Color:** #4caf50 (var(--pass))
- **Background:** #E8F5E9 (var(--pass-bg))
- **Usage:** Positive indicators, success states, compliance

**Error (Fail):**
- **Color:** #c62828 (var(--fail))
- **Background:** #FFEBEE (var(--fail-bg))
- **Usage:** Error states, failures, non-compliance

**Warning (Rework):**
- **Color:** #ef6c00 (var(--warning))
- **Background:** #FFF3E0 (var(--warning-bg))
- **Usage:** Warnings, rework states, attention needed

**Info:**
- **Color:** #0a8276 (var(--info))
- **Background:** #E0F2F1 (var(--info-bg))
- **Usage:** Informational content, neutral states

**Neutral:**
- **Color:** #5F6368 (var(--neutral))
- **Background:** #F1F3F4 (var(--neutral-bg))
- **Usage:** Disabled states, inactive elements

### 5.4 Neutral Colors

**Background Colors:**
- **Background:** #F8F9FA (var(--bg))
- **Background Gradient:** #E9ECEF (var(--bg-grad))
- **Surface:** #FFFFFF (var(--surface))
- **Surface 2:** #F1F3F4 (var(--surface-2))
- **Surface 3:** #E8EAED (var(--surface-3))
- **Panel:** #FAFBFC (var(--panel))

**Text Colors:**
- **Ink:** #202124 (var(--ink))
- **Ink Soft:** #5F6368 (var(--ink-soft))
- **Muted:** #9AA0A6 (var(--muted))

**Border Colors:**
- **Hair:** #DADCE0 (var(--hair))
- **Hair Strong:** #BDC1C6 (var(--hair-strong))
- **Panel Hair:** #DADCE0 (var(--panel-hair))

### 5.5 Color Usage Rules

**Primary Color (#0a8276):**
- Primary buttons, links
- Active navigation items
- Focus states
- Brand accents
- Highlighted information

**Secondary Colors:**
- **Orange:** Warnings, attention states, drying-related metrics
- **Red:** Errors, failures, non-compliance, critical alerts
- **Green:** Success, compliance, positive trends, sustainability metrics

**Status Colors:**
- **Success:** Completed, compliant, pass, approved
- **Error:** Failed, rejected, non-compliant, error states
- **Warning:** Rework, pending, attention needed, borderline
- **Info:** Informational, neutral, processing
- **Neutral:** Disabled, inactive, not applicable

**Semantic Color Mapping:**
- **Production:** Primary teal (#0a8276)
- **Quality:** Green (#4caf50) for pass, red (#c62828) for fail
- **Energy:** Orange (#ef6c00) for consumption
- **Water:** Blue (#0a8276) for efficiency
- **Sustainability:** Green (#4caf50) for eco metrics

---

## 06. COMPONENT LIBRARY

### 6.1 Buttons

**Button Base Styles:**
```css
.btn {
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-md);
  font-weight: 500;
  font-size: 0.875rem;
  border: none;
  cursor: pointer;
  transition: var(--transition-fast);
}
```

**Button Variants:**

**Primary Button:**
- **Background:** #0a8276 (var(--primary))
- **Text:** White
- **Hover:** #075752 (var(--primary-hover))
- **Usage:** Primary actions, form submissions, main CTAs

**Secondary Button:**
- **Background:** #FFFFFF (var(--surface))
- **Border:** #DADCE0 (var(--hair))
- **Text:** #202124 (var(--ink))
- **Hover:** #F1F3F4 (var(--surface-2))
- **Usage:** Secondary actions, cancel, back

**Success Button:**
- **Background:** #4caf50 (var(--pass))
- **Text:** White
- **Hover:** #3d8b40 (var(--secondary-green-soft))
- **Usage:** Confirm, approve, success actions

**Danger Button:**
- **Background:** #c62828 (var(--fail))
- **Text:** White
- **Hover:** #a02020 (var(--secondary-red-soft))
- **Usage:** Delete, reject, destructive actions

**Button Sizes:**
- **Small:** padding: 4px 8px, font-size: 12px
- **Medium:** padding: 8px 16px, font-size: 14px (default)
- **Large:** padding: 16px 24px, font-size: 16px

**Button States:**
- **Default:** Normal appearance
- **Hover:** Darker background (10% darker)
- **Active:** Pressed state (15% darker)
- **Disabled:** 50% opacity, not-allowed cursor
- **Loading:** Spinner with disabled state

### 6.2 Cards

**Card Base Styles:**
```css
.card {
  background: var(--surface);
  border: 1px solid var(--hair);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  box-shadow: var(--shadow-sm);
}
```

**Dashboard Card (KPI Card):**
- **Background:** #FFFFFF (var(--surface))
- **Border:** #DADCE0 (var(--hair))
- **Border Radius:** 12px (var(--radius-lg))
- **Padding:** 24px (var(--space-lg))
- **Shadow:** 0 1px 2px rgba(0,0,0,0.05)
- **Label Font:** 12px, uppercase, #9AA0A6 (var(--muted))
- **Value Font:** 24px, 600 weight, #202124 (var(--ink))
- **Trend Font:** 12px, #5F6368 (var(--ink-soft))

**Card Content Structure:**
```html
<div class="dash-card">
  <div class="label">KPI Name</div>
  <div class="value">KPI Value</div>
  <div class="trend">Trend indicator</div>
</div>
```

**Card Variants:**
- **Default:** Standard card with border
- **Elevated:** Larger shadow (var(--shadow-md))
- **Compact:** Reduced padding (var(--space-md))
- **Interactive:** Hover state with var(--shadow-md)

### 6.3 Tables

**Table Container:**
```css
.table-container {
  background: var(--surface);
  border: 1px solid var(--hair);
  border-radius: var(--radius-lg);
  overflow: hidden;
}
```

**Table Header:**
- **Background:** #F1F3F4 (var(--surface-2))
- **Padding:** 16px 24px (var(--space-md) var(--space-lg))
- **Border Bottom:** #DADCE0 (var(--hair))
- **Title Font:** 14px, 600 weight, #202124 (var(--ink))

**Table Cells:**
- **Padding:** 12px 16px (var(--space-sm) var(--space-md))
- **Border Bottom:** #DADCE0 (var(--hair))
- **Font:** 14px, #202124 (var(--ink))
- **Hover Background:** #F8F9FA (var(--bg))

**Table Sort:**
- **Sortable Headers:** Add cursor pointer
- **Sort Indicator:** Arrow icon (↑/↓)
- **Active Sort:** #0a8276 (var(--primary)) color

**Table Responsive:**
- **Mobile:** Horizontal scroll for overflow
- **Breakpoint:** < 768px
- **Minimum Width:** 600px

### 6.4 Badges

**Badge Base Styles:**
```css
.badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: 0.75rem;
  font-weight: 500;
}
```

**Badge Variants:**
- **Success:** #E8F5E9 background, #4caf50 text
- **Warning:** #FFF3E0 background, #ef6c00 text
- **Error:** #FFEBEE background, #c62828 text
- **Info:** #E0F2F1 background, #0a8276 text
- **Neutral:** #F1F3F4 background, #5F6368 text
- **Brand:** #E0F2F1 background, #0a8276 text
- **Sustainability:** #E8F5E9 background, #4caf50 text
- **Orange:** #FFF3E0 background, #ef6c00 text

**Badge Usage:**
- **Status Indicators:** Active/inactive, pass/fail
- **Categories:** Equipment types, process stages
- **Priority:** High/medium/low
- **Metadata:** Tags, labels

### 6.5 Forms

**Form Grid:**
```css
.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--space-md);
}
```

**Form Group:**
```css
.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
```

**Form Label:**
- **Font:** 14px, 500 weight, #202124 (var(--ink))
- **Required Indicator:** Red asterisk (#c62828)
- **Margin Bottom:** 4px (var(--space-xs))

**Form Input:**
```css
.form-input {
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--hair);
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  font-family: var(--font-body);
}
```

**Form Input States:**
- **Default:** #DADCE0 border
- **Focus:** #0a8276 border, outline none
- **Error:** #c62828 border, red shadow
- **Disabled:** #DADCE0 border, 50% opacity

**Form Error:**
- **Font:** 12px, #c62828 (var(--fail))
- **Margin Top:** 4px (var(--space-xs))

**Form Hint:**
- **Font:** 12px, #9AA0A6 (var(--muted))
- **Margin Top:** 4px (var(--space-xs))

### 6.6 Navigation

**Sidebar Navigation:**
- **Width:** 280px
- **Background:** #0a8276 (var(--primary))
- **Height:** 100vh (full viewport height)
- **Position:** Fixed, left side
- **Z-Index:** 200 (var(--z-sticky))

**Nav Section:**
- **Font:** 12px, uppercase, #9AA0A6 (var(--muted))
- **Margin Top:** 24px (var(--space-lg))
- **Margin Bottom:** 8px (var(--space-sm))
- **Padding:** 0 16px (var(--space-md))

**Nav Item:**
- **Height:** 40px
- **Padding:** 0 16px (var(--space-md))
- **Display:** Flex, align-items center
- **Gap:** 12px
- **Color:** rgba(255,255,255,0.8)
- **Hover:** rgba(255,255,255,1)
- **Active:** rgba(255,255,255,1), background rgba(255,255,255,0.1)

**Nav Icon:**
- **Font:** Material Icons
- **Size:** 20px
- **Color:** rgba(255,255,255,0.8)

---

## 07. CHART VISUALIZATION STANDARDS

### 7.1 Chart Requirements

**X-Axis Requirements:**
- **Label:** Must clearly identify the dimension being displayed
- **Format:** Examples: "Year", "Month", "Batch", "Drying Cycle", "Week"
- **Position:** Bottom of chart, left-aligned
- **Font:** 12px, #5F6368 (var(--ink-soft))
- **Rotation:** 0° (horizontal) for readability

**Y-Axis Requirements:**
- **Label:** Must clearly identify the metric and unit
- **Format:** Examples: "Production (kg)", "Energy Consumption (kWh)", "Water Consumption (L)", "Quality (%)"
- **Position:** Left of chart, rotated 90°
- **Font:** 12px, #5F6368 (var(--ink-soft))
- **Units:** Always include units in parentheses

**Axis Line Styles:**
- **Color:** #DADCE0 (var(--hair))
- **Width:** 1px
- **Grid Lines:** Horizontal only, light gray (#E8EAED)

### 7.2 Chart Types and Standards

**Production Charts:**
- **X-Axis:** Time dimension (Year, Month, Week, Batch)
- **Y-Axis:** Production (kg)
- **Color:** #0a8276 (var(--primary))
- **Chart Type:** Line chart for trends, bar chart for comparison

**Energy Charts:**
- **X-Axis:** Time dimension (Day, Week, Month)
- **Y-Axis:** Energy Consumption (kWh)
- **Color:** #ef6c00 (var(--secondary-orange))
- **Chart Type:** Line chart for consumption, bar chart for comparison

**Water Charts:**
- **X-Axis:** Time dimension (Day, Week, Month)
- **Y-Axis:** Water Consumption (L)
- **Color:** #0a8276 (var(--primary))
- **Chart Type:** Line chart for usage, bar chart for efficiency

**Quality Charts:**
- **X-Axis:** Quality Grade or Time dimension
- **Y-Axis:** Percentage (%) or Count
- **Color:** #4caf50 (var(--pass)) for pass, #c62828 (var(--fail)) for fail
- **Chart Type:** Pie chart for distribution, bar chart for trends

### 7.3 Chart Labels and Units

**Label Format Rules:**
- **Complete Labels:** "Production (kg)" not "Production"
- **Clear Units:** Always include units in parentheses
- **No Abbreviations:** "Kilowatt-hours" not "kWh" in axis labels
- **Consistent Terminology:** Use same terms throughout dashboard

**Forbidden Labels:**
- Ambiguous labels like "5m" (meaning unclear)
- Unit-less metrics without clear context
- Abbreviations without explanation
- Technical jargon without definition

**Chart Title Standards:**
- **Format:** Clear, descriptive title
- **Font:** 16px, 600 weight, #202124 (var(--ink))
- **Position:** Above chart, left-aligned
- **Margin:** 16px bottom margin

**Legend Standards:**
- **Position:** Right side or bottom
- **Font:** 12px, #5F6368 (var(--ink-soft))
- **Markers:** Consistent with chart colors
- **Spacing:** 8px between items

### 7.4 Chart Data Display

**Value Labels:**
- **Font:** 12px, 500 weight
- **Color:** #202124 (var(--ink))
- **Position:** On data points or bars
- **Format:** Consistent with axis units

**Trend Indicators:**
- **Up Arrow:** #4caf50 (var(--pass)) for positive
- **Down Arrow:** #c62828 (var(--fail)) for negative
- **Neutral:** #5F6368 (var(--neutral)) for no change
- **Percentage:** Show relative change with sign (+8%, -3%)

**Tooltip Standards:**
- **Background:** #FFFFFF (var(--surface))
- **Border:** #DADCE0 (var(--hair))
- **Shadow:** var(--shadow-lg)
- **Padding:** 12px
- **Font:** 12px, #202124 (var(--ink))
- **Content:** Label, value, unit, context

---

## 08. SPACING SYSTEM

### 8.1 Spacing Scale

**Rem-Based Spacing:**
- **Extra Small (xs):** 0.25rem (4px)
- **Small (sm):** 0.5rem (8px)
- **Medium (md):** 1rem (16px)
- **Large (lg):** 1.5rem (24px)
- **Extra Large (xl):** 2rem (32px)
- **Double Extra Large (2xl):** 3rem (48px)

### 8.2 Spacing Usage Rules

**Component Padding:**
- **Cards:** 24px (var(--space-lg))
- **Tables:** 12px 16px (var(--space-sm) var(--space-md))
- **Forms:** 16px (var(--space-md))
- **Modals:** 24px (var(--space-lg))

**Component Margins:**
- **Section Margin:** 32px (var(--space-xl))
- **Card Margin:** 16px (var(--space-md))
- **Element Spacing:** 8px (var(--space-sm))

**Gap Between Elements:**
- **Form Elements:** 8px (var(--space-sm))
- **Button Groups:** 8px (var(--space-sm))
- **Grid Gaps:** 16px (var(--space-md))

---

## 09. BORDER RADIUS

### 9.1 Radius Scale

**Border Radius Values:**
- **Small (sm):** 4px
- **Medium (md):** 8px
- **Large (lg):** 12px
- **Extra Large (xl):** 16px
- **Full:** 9999px (circle)

### 9.2 Radius Usage Rules

**Small Radius (4px):**
- Input fields
- Small buttons
- Tags

**Medium Radius (8px):**
- Standard buttons
- Form inputs
- Small cards

**Large Radius (12px):**
- Cards
- Modals
- Tables

**Extra Large Radius (16px):**
- Hero sections
- Large modals
- Featured cards

**Full Radius (9999px):**
- Badges
- Pills
- Circular avatars

---

## 10. SHADOWS

### 10.1 Shadow Scale

**Shadow Values:**
- **Small (sm):** 0 1px 2px rgba(0,0,0,0.05)
- **Medium (md):** 0 2px 8px rgba(0,0,0,0.08)
- **Large (lg):** 0 4px 16px rgba(0,0,0,0.12)
- **Extra Large (xl):** 0 8px 32px rgba(0,0,0,0.16)

### 10.2 Shadow Usage Rules

**Small Shadow:**
- Cards (default)
- Dropdowns
- Tooltips

**Medium Shadow:**
- Elevated cards
- Modals
- Active states

**Large Shadow:**
- Hero sections
- Featured elements
- Active dropdowns

**Extra Large Shadow:**
- Modals with backdrop
- Page overlays
- Critical notifications

---

## 11. TRANSITIONS

### 11.1 Transition Scale

**Transition Values:**
- **Fast:** 150ms ease
- **Normal:** 250ms ease
- **Slow:** 350ms ease

### 11.2 Transition Usage Rules

**Fast Transitions (150ms):**
- Button hover states
- Input focus states
- Badge hover

**Normal Transitions (250ms):**
- Card hover states
- Modal open/close
- Dropdown open/close

**Slow Transitions (350ms):**
- Page transitions
- Complex animations
- Hero section changes

---

## 12. ICONS

### 12.1 Icon System

**Icon Library:** Material Icons (Google Fonts)

**Icon Loading:**
```html
<link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">
```

**Icon Sizes:**
- **Small:** 16px
- **Medium:** 20px (default)
- **Large:** 24px
- **Extra Large:** 32px

### 12.2 Icon Usage Rules

**Color Standards:**
- **Primary:** #0a8276 (var(--primary))
- **Secondary:** #5F6368 (var(--ink-soft))
- **Success:** #4caf50 (var(--pass))
- **Error:** #c62828 (var(--fail))
- **Warning:** #ef6c00 (var(--warning))

**Icon Categories:**
- **Navigation:** agriculture, inventory, water_drop, wb_sunny, verified, warehouse, local_shipping
- **Status:** check_circle, cancel, warning, info
- **Actions:** add, edit, delete, search, filter
- **Trends:** trending_up, trending_down, trending_flat

---

## 13. RESPONSIVE BEHAVIOR

### 13.1 Breakpoints

**Breakpoint Scale:**
- **Mobile:** < 768px
- **Tablet:** 768px - 1024px
- **Desktop:** > 1024px

### 13.2 Responsive Rules

**Mobile (< 768px):**
- **Sidebar:** Hidden (hamburger menu)
- **Grid:** Single column
- **Tables:** Horizontal scroll
- **Cards:** Full width
- **Font:** Base size maintained

**Tablet (768px - 1024px):**
- **Sidebar:** Collapsible
- **Grid:** 2 columns
- **Tables:** Responsive with horizontal scroll
- **Cards:** 2 per row
- **Font:** Base size maintained

**Desktop (> 1024px):**
- **Sidebar:** Fixed 280px width
- **Grid:** 3-4 columns
- **Tables:** Full width
- **Cards:** 3-4 per row
- **Font:** Base size maintained

### 13.3 Mobile Optimizations

**Touch Targets:**
- **Minimum Size:** 44px × 44px
- **Spacing:** 8px minimum between touch targets

**Readable Text:**
- **Minimum Size:** 16px
- **Line Height:** 1.5
- **Contrast:** WCAG AA compliant

---

## 14. SEARCH FUNCTIONALITY

### 14.1 Search Component Standards

**Search Input:**
- **Placeholder:** "Search..." or context-specific placeholder
- **Icon:** Search icon in left side
- **Clear Button:** X icon to clear search
- **Width:** 100% (responsive)

**Search Behavior:**
- **Real-time:** Filter as user types (optional)
- **On Submit:** Search on Enter key (required)
- **Case Insensitive:** Ignore case for text matching
- **Debounce:** 300ms delay for real-time search

**Search Results:**
- **Highlight:** Highlight matching text
- **Empty State:** "No results found" message
- **Loading State:** Spinner during search

### 14.2 Search Implementation

**Search Bar Component:**
```html
<div class="table-search">
  <input type="text" placeholder="Search batches..." />
</div>
```

**Search Styles:**
```css
.table-search input {
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--hair);
  border-radius: var(--radius-md);
  width: 100%;
  min-width: 200px;
}
```

---

## 15. DARK THEME

### 15.1 Dark Theme Standards

**Color Inversion:**
- **Background:** #202124 (instead of #F8F9FA)
- **Surface:** #2D2E30 (instead of #FFFFFF)
- **Text:** #F8F9FA (instead of #202124)
- **Borders:** #3C4043 (instead of #DADCE0)

**Implementation:**
- **Toggle:** Theme toggle in sidebar
- **Persistence:** Save preference in localStorage
- **Transition:** Smooth transition (300ms)

### 15.2 Dark Theme Color Mapping

**Light → Dark Mapping:**
- --bg: #F8F9FA → #202124
- --surface: #FFFFFF → #2D2E30
- --surface-2: #F1F3F4 → #3C4043
- --ink: #202124 → #F8F9FA
- --ink-soft: #5F6368 → #B8BCC1
- --muted: #9AA0A6 → #7A7F82
- --hair: #DADCE0 → #3C4043

**Status Colors:** Maintain same values in dark theme
**Brand Colors:** Maintain same values in dark theme

---

## 16. DESIGN SYSTEM IMPLEMENTATION

### 16.1 CSS Variables

**Root Variables:**
```css
:root {
  /* Core Colors */
  --bg: #F8F9FA;
  --surface: #FFFFFF;
  --ink: #202124;
  --ink-soft: #5F6368;
  --muted: #9AA0A6;
  --hair: #DADCE0;
  
  /* Brand Colors */
  --primary: #0a8276;
  --primary-soft: #086b61;
  --primary-tint: #E0F2F1;
  --primary-hover: #075752;
  
  /* Status Colors */
  --pass: #4caf50;
  --pass-bg: #E8F5E9;
  --fail: #c62828;
  --fail-bg: #FFEBEE;
  --warning: #ef6c00;
  --warning-bg: #FFF3E0;
  
  /* Typography */
  --font-body: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  --font-display: 'Montserrat', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  
  /* Spacing */
  --space-xs: 0.25rem;
  --space-sm: 0.5rem;
  --space-md: 1rem;
  --space-lg: 1.5rem;
  --space-xl: 2rem;
  
  /* Border Radius */
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 16px;
  --radius-full: 9999px;
  
  /* Shadows */
  --shadow-sm: 0 1px 2px rgba(0,0,0,0.05);
  --shadow-md: 0 2px 8px rgba(0,0,0,0.08);
  --shadow-lg: 0 4px 16px rgba(0,0,0,0.12);
  --shadow-xl: 0 8px 32px rgba(0,0,0,0.16);
  
  /* Transitions */
  --transition-fast: 150ms ease;
  --transition-normal: 250ms ease;
  --transition-slow: 350ms ease;
}
```

### 16.2 Component Implementation

**React Component Structure:**
```javascript
// Design system components
import './DesignSystem.css';

const Button = ({ variant = 'primary', size = 'medium', children, ...props }) => {
  return (
    <button 
      className={`btn btn-${variant} btn-${size}`}
      {...props}
    >
      {children}
    </button>
  );
};

const Card = ({ children, className = '', ...props }) => {
  return (
    <div className={`card ${className}`} {...props}>
      {children}
    </div>
  );
};

const Badge = ({ variant = 'info', children }) => {
  return (
    <span className={`badge badge-${variant}`}>
      {children}
    </span>
  );
};
```

---

## 17. WEEK 5 FEEDBACK COMPLIANCE

### 17.1 Logo Compliance

**Feedback:** "Logo must have appropriate/white background and adequate size"

**Compliance:** ✅ COMPLIANT
- Logo specifications documented with white background requirement
- Size standards defined (40px standard, 32px minimum, 64px maximum)
- Clear space rules established
- Usage guidelines provided

### 17.2 Typography Compliance

**Feedback:** "Typography, colors and component styles must be consistent"

**Compliance:** ✅ COMPLIANT
- Typography hierarchy formalized (6 heading levels, 4 body sizes)
- Font families standardized (Inter, Montserrat, SF Mono)
- Font weights defined (400, 500, 600, 700)
- Color palette standardized (brand, secondary, status, neutral)
- Component library created (buttons, cards, tables, badges, forms)

### 17.3 Chart Compliance

**Feedback:** "Charts must clearly describe X/Y axes and units"

**Compliance:** ✅ COMPLIANT
- X-axis standards defined (dimension labels, horizontal orientation)
- Y-axis standards defined (metric + unit format, rotated orientation)
- Forbidden formats identified (ambiguous labels, abbreviations)
- Chart type guidelines provided
- Legend and tooltip standards defined

### 17.4 Feature Preservation

**Feedback:** "Search functionality is a good idea"

**Compliance:** ✅ PRESERVED
- Search component standards documented
- Search behavior defined (real-time, on-submit, case-insensitive)
- Search result handling specified

**Feedback:** "Dark theme is a good idea"

**Compliance:** ✅ PRESERVED
- Dark theme color mapping defined
- Toggle implementation specified
- Persistence and transition standards established

---

## 18. DESIGN SYSTEM GOVERNANCE

### 18.1 Usage Rules

**Component Usage:**
- **Required:** Use design system components for all new UI elements
- **Customization:** Only customize within design system parameters
- **Consistency:** Maintain consistent application across all pages

**Color Usage:**
- **Required:** Use CSS variables for all colors
- **Forbidden:** Hardcoded color values (except in CSS variable definitions)
- **Semantic:** Use colors according to their semantic meaning

**Typography Usage:**
- **Required:** Use defined font families and sizes
- **Forbidden:** Arbitrary font sizes or families
- **Hierarchy:** Follow heading hierarchy strictly

### 18.2 Maintenance

**Updates:**
- **Process:** Design system changes must be documented in this file
- **Testing:** Design system changes must be tested across all components
- **Communication:** Notify team of design system updates

**Version Control:**
- **File:** DESIGN_SYSTEM.md is the source of truth
- **Version:** Maintain version history for major changes
- **Approval:** Major changes require team approval

---

## 19. IMPLEMENTATION CHECKLIST

### 19.1 Week 6 Design Tasks

**High Priority:**
- [x] Extract design system from mock-up CSS variables
- [x] Document logo specifications
- [x] Standardize typography hierarchy
- [x] Define color usage rules
- [x] Create component style guide
- [x] Define chart visualization standards
- [x] Document spacing and layout rules
- [x] Define responsive behavior

**Implementation Tasks:**
- [ ] Create React design system component library
- [ ] Apply design system to existing frontend components
- [ ] Implement search functionality with design system standards
- [ ] Implement dark theme toggle with design system colors
- [ ] Test design system across all components
- [ ] Validate Week 5 feedback compliance

---

## 20. NEXT STEPS

### 20.1 Immediate Actions

1. **Create React Component Library:**
   - Implement Button, Card, Badge, Table components
   - Create design system CSS file with all variables
   - Build component storybook or documentation

2. **Apply Design System:**
   - Update existing React components to use design system
   - Replace hardcoded styles with CSS variables
   - Ensure consistent application across all pages

3. **Implement Features:**
   - Build search component per design system standards
   - Implement dark theme toggle per color mapping
   - Add responsive behavior per breakpoint rules

### 20.2 Validation

1. **Visual Testing:**
   - Verify logo treatment (white background, appropriate size)
   - Validate typography consistency across all pages
   - Check color usage follows semantic rules

2. **Chart Testing:**
   - Verify all charts have proper axis labels with units
   - Validate chart colors follow defined palette
   - Test legend and tooltip standards

3. **Feature Testing:**
   - Test search functionality per design system standards
   - Validate dark theme color mapping
   - Test responsive behavior at breakpoints

---

**Design System Status:** ✅ FORMALIZED  
**Week 5 Feedback Compliance:** ✅ FULLY COMPLIANT  
**Component Library:** ✅ SPECIFIED  
**Chart Standards:** ✅ DEFINED  
**Ready for Implementation:** YES