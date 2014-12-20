package org.openhab.io.habmin.services.sitemap;

/**
 * Specifies the conditional statements used in the sitemaps
 * @author Chris Jackson
 * @since 1.4.0
 *
 */
public enum SitemapCondition {
	EQUAL("=="), GTE(">="), LTE("<="), NOTEQUAL("!="), GREATER(">"), LESS("<"), NOT("!");

	private String value;

	private SitemapCondition(String value) {
		this.value = value;
	}

	public static SitemapCondition fromString(String text) {
		if (text != null) {
			for (SitemapCondition c : SitemapCondition.values()) {
				if (text.equalsIgnoreCase(c.value)) {
					return c;
				}
			}
		}
		return null;
	}

	public String toString() {
		return this.value;
	}
}
