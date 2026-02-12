package com.veritynow.ms.ocr.docai;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.cloud.documentai.v1.Document;
import com.google.cloud.documentai.v1.Document.Page;
import com.google.cloud.documentai.v1.ProcessResponse;
import com.veritynow.ms.ocr.OcrField;
import com.veritynow.ms.pdf.FieldInfo;
import com.veritynow.ms.pdf.FormTemplate;
import com.veritynow.util.JSON;

public class DocAiOutputParser {

	static Map<String, OcrField> parse(ProcessResponse response, FormTemplate t, List<String> selected) {

		try {

			Document doc = response.getDocument();

			String allText = doc.getText();

			// Pre-build a normalized map of template field IDs for better matching
			Map<String, String> normalizedFieldIdMap = buildNormalizedFieldIdMap(t);

			// NEW: Build template-driven date triplet index
			Map<String, DateTriplet> dateTripletIndex = buildDateTripletIndex(t);

			Map<String, OcrField> results = new LinkedHashMap<>();

			// 6) Walk all pages / form fields from DocAI
			for (Page page : doc.getPagesList()) {
				for (com.google.cloud.documentai.v1.Document.Page.FormField ff : page.getFormFieldsList()) {

					String nameText = getText(ff.getFieldName().getTextAnchor(), allText);
					String valueText = getText(ff.getFieldValue().getTextAnchor(), allText);

					if (nameText == null || nameText.isBlank()) {
						continue;
					}
					
					nameText = normalize(nameText);
					if (valueText != null) {
						valueText = normalize(valueText);
					}
					
					String matchedFieldId = matchFieldId(normalizedFieldIdMap, nameText, selected);
					
					if (matchedFieldId == null) {
						matchedFieldId = nameText;
					}

					double confidence = 0.0;
					// In newer DocAI API, confidence is on the fieldValue layout
					if (ff.hasFieldValue()) {
						confidence = ff.getFieldValue().getConfidence();
					}

					String cleaned = valueText != null ? valueText.trim() : "";

					// NEW: If this matched field belongs to a template-defined date triplet
					// and OCR gave a combined date string, split it into Month/Day/Year siblings.
					DateTriplet triplet = dateTripletIndex.get(matchedFieldId);
					if (triplet != null && isDateLike(cleaned)) {
						int[] parts = parseDateParts(cleaned);
						if (parts != null) {
							results.put(triplet.monthFieldId,
									new OcrField(triplet.monthFieldId, pad2(parts[0]), confidence));
							results.put(triplet.dayFieldId,
									new OcrField(triplet.dayFieldId, pad2(parts[1]), confidence));
							results.put(triplet.yearFieldId,
									new OcrField(triplet.yearFieldId, String.valueOf(parts[2]), confidence));
							continue; // IMPORTANT: don't also store the combined date under matchedFieldId
						}
					}

					// Default behavior (non-date or not triplet)
					if (matchedFieldId != null) {
						nameText = matchedFieldId;
					}
					results.put(matchedFieldId, new OcrField(nameText, cleaned, confidence));
				}

			}

			System.out.println("OCR Results " + JSON.writeValueAsString(results));
			return results;

		} catch (Exception e) {
			throw new RuntimeException();
		}

	}

// ---------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------

// ---------------------------------------------------------------------
// Template-driven date triplet support
// ---------------------------------------------------------------------

	private static class DateTriplet {
		final String monthFieldId;
		final String dayFieldId;
		final String yearFieldId;

		DateTriplet(String base, String monthFieldId, String dayFieldId, String yearFieldId) {
			this.monthFieldId = monthFieldId;
			this.dayFieldId = dayFieldId;
			this.yearFieldId = yearFieldId;
		}
	}

	/**
	 * Build an index of template-defined date triplets.
	 *
	 * We only consider triplets that actually exist in cfg.getZones(). So we aren't
	 * "guessing" based on suffix alone — the template is the authority.
	 */
	private static Map<String, DateTriplet> buildDateTripletIndex(FormTemplate t) {
		Map<String, DateTriplet> index = new LinkedHashMap<>();
		if (t.fields() == null)
			return index;

		List<String> ids = List.copyOf(t.fields().keySet()).stream().sorted((f1,f2) -> f1.compareTo(f2)).toList(); 
		
		// Use set for O(1) sibling checks
		Set<String> idSet = new HashSet<>(ids);

		for (String id : ids) {
			if (!id.endsWith(" Month"))
				continue;

			String base = id.substring(0, id.length() - " Month".length());
			String dayId = base + " Day";
			String yearId = base + " Year";

			// Only accept if siblings exist in the template
			if (idSet.contains(dayId) && idSet.contains(yearId)) {
				DateTriplet t1 = new DateTriplet(base, id, dayId, yearId);

				// map all 3 ids to same triplet
				index.put(id, t1);
				index.put(dayId, t1);
				index.put(yearId, t1);
			}
		}

		return index;
	}

// ---------------------------------------------------------------------
// Date parsing helpers (OCR tolerant)
// ---------------------------------------------------------------------

	private static boolean isDateLike(String v) {
		if (v == null)
			return false;
		String s = v.trim();
		return s.matches(".*\\d{1,2}\\s*[\\/\\-\\.]\\s*\\d{1,2}\\s*[\\/\\-\\.]\\s*\\d{2,4}.*")
				|| s.matches(".*\\d{1,2}\\s+\\d{1,2}\\s+\\d{2,4}.*");
	}

	private static String pad2(int n) {
		return (n < 10 ? "0" : "") + n;
	}

	private static int normalizeYear(int y) {
		if (y < 100)
			return (y >= 50 ? 1900 + y : 2000 + y);
		return y;
	}

	/**
	 * Returns int[]{mm, dd, yyyy} or null. Heuristic: - If first component > 12 and
	 * second <= 12 => dd/mm/yyyy - Else => mm/dd/yyyy
	 */
	private static int[] parseDateParts(String raw) {
		if (raw == null)
			return null;
		String s = raw.trim();

		String[] parts = s.split("[\\/\\-\\.]");
		if (parts.length < 3)
			parts = s.split("\\s+");
		if (parts.length < 3)
			return null;

		try {
			int a = Integer.parseInt(parts[0].trim());
			int b = Integer.parseInt(parts[1].trim());
			int c = Integer.parseInt(parts[2].trim());

			int year = normalizeYear(c);

			int mm = a, dd = b;
			if (a > 12 && b <= 12) {
				dd = a;
				mm = b;
			}

			return new int[] { mm, dd, year };
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Extract text from a DocAI TextAnchor using the full document text.
	 */
	private static String getText(Document.TextAnchor anchor, String allText) {
		if (anchor == null || anchor.getTextSegmentsCount() == 0) {
			return "";
		}
		// Many segments possible; for forms usually the first segment is enough
		int start = (int) anchor.getTextSegments(0).getStartIndex();
		int end = (int) anchor.getTextSegments(0).getEndIndex();
		if (start < 0 || end > allText.length() || start >= end) {
			return "";
		}
		return allText.substring(start, end);
	}

	/**
	 * Builds a map of normalizedName -> originalFieldId from the template's zone
	 * keys.
	 */
	@SuppressWarnings("unused")
	private static Map<String, String> buildNormalizedFieldIdMap(FormTemplate t) {
		Map<String, String> map = new TreeMap<>();
		if (t.fields() == null)
			return map;

		Map<String, FieldInfo> fields = t.fields();
		
		System.out.println(fields.size() + " FIELDS SIZE ====================");
		
		fields.forEach((k,f) -> {
			String norm = normalize(k);
			map.put(norm, k);
		}); 
		
		System.out.println(map.size() + " MAP SIZE ====================");
		return map;
	}

	/**
	 * Attempts to match a Google form field name like "First Name" or "Home Address
	 * Barangay" to one of our template fieldIds, using a simple normalization +
	 * contains strategy.
	 */
	private static String matchFieldId(Map<String, String> normalizedFieldIdMap, String googleFieldName, List<String> selected) {
		String normGoogle = normalize(googleFieldName);
		if (normGoogle.isBlank())
			return null;

		// 1) Exact normalized match
		if (normalizedFieldIdMap.containsKey(normGoogle)) {
			return normalizedFieldIdMap.get(normGoogle);
		}

		// 2) Contains / ends with / starts with heuristics
		String bestMatch = null;
		int bestScore = 0;

		for (Map.Entry<String, String> e : normalizedFieldIdMap.entrySet()) {
			String normTemplate = e.getKey();
			
			if (normTemplate.contains(" gross ")) {
				System.out.println("normailaized " + normTemplate);
			}

			String value = e.getValue();
			int score = similarityScore(normTemplate, normGoogle);
			if (score > bestScore && !(value != null && selected.contains(value))) {
				bestScore = score;
				bestMatch = e.getValue();
				
			}
		}

		if (normGoogle.toLowerCase().contains("middle")) {
			System.out.println("best match -> " + bestMatch);
		}
		// Optional: require a minimum score to avoid silly matches
		if (bestScore < 2) {
			return null;
		}
		selected.add(bestMatch);
		return bestMatch;
	}

	/**
	 * Very cheap similarity scoring based on token overlap. Not perfect, but good
	 * enough for "First Name", "Last Name", etc.
	 */
	private static int similarityScore(String a, String b) {
		String[] ta = a.toLowerCase().split("\\s+");
		String[] tb = b.toLowerCase().split("\\s+");
		int score = 0;
		for (String sa : ta) {
			for (String sb : tb) {
				//fix spelling
				if (sa.equals("monthy")) {
					sa = "monthly";
				}
				if (sb.equals("monthy")) {
					sb = "monthly";
				}
				if (sa.equals(sb)) {
					score++;
				}
			}
		}
		
		return score;
	}

	private static String normalize(String s) {
		if (s == null)
			return "";
		// Lowercase, keep letters/digits/spaces, collapse spaces
		String cleaned = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\n]+", " ").trim();
		return cleaned.replaceAll("\\s+", " ");
	}

}
