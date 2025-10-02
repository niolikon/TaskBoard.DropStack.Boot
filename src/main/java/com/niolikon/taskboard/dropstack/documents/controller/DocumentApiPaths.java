package com.niolikon.taskboard.dropstack.documents.controller;

public class DocumentApiPaths {
    private DocumentApiPaths() {}

    public static final String PATH_VARIABLE_DOCUMENT_ID = "id";
    public static final String PART_NAME_FILE = "file";
    public static final String PART_NAME_METADATA = "metadata";

    public static final String MAPPING_PATH_DOCUMENT_BASE = "/api/Documents";
    public static final String MAPPING_PATH_DOCUMENT_BY_ID = "/{" + PATH_VARIABLE_DOCUMENT_ID + "}";
    public static final String MAPPING_PATH_DOCUMENT_CONTENT_BY_ID = "/{" + PATH_VARIABLE_DOCUMENT_ID + "}/content";
    public static final String MAPPING_PATH_DOCUMENT_CHECKIN_BY_ID = "/{" + PATH_VARIABLE_DOCUMENT_ID + "}/checkin";

    public static final String API_PATH_DOCUMENT_BASE = MAPPING_PATH_DOCUMENT_BASE;
    public static final String API_PATH_DOCUMENT_BY_ID = MAPPING_PATH_DOCUMENT_BASE + MAPPING_PATH_DOCUMENT_BY_ID;
    public static final String API_PATH_DOCUMENT_CONTENT_BY_ID = MAPPING_PATH_DOCUMENT_BASE + MAPPING_PATH_DOCUMENT_CONTENT_BY_ID;

    public static final String SECURITY_PATTERN_DOCUMENT_EXACT = MAPPING_PATH_DOCUMENT_BASE;
    public static final String SECURITY_PATTER_DOCUMENT_ALL = MAPPING_PATH_DOCUMENT_BASE + "/**";
}
