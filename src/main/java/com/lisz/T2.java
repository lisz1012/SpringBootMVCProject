package com.lisz;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.LocaleUtils;

import java.util.Locale;

public class T2 {
	public static void main(String[] args) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(new Type("marketplaceId", LocaleUtils.toLocale("en_US")));
		System.out.println(json);
		Type type = mapper.readValue(json, Type.class);
		String marketPlaceId = type.getMarketPlaceId();
		System.out.println(marketPlaceId);
		Locale locale = type.getLocale();
		System.out.println(locale);
	}

	@AllArgsConstructor
	@Data
	private static class Type {
		//@JsonProperty
		private String marketPlaceId;
		//@JsonProperty
		private Locale locale;

		public Type() {
		}

		@Override
		public String toString() {
			return "Type{" +
					"marketPlaceId='" + marketPlaceId + '\'' +
					", locale=" + locale +
					'}';
		}
	}
}
