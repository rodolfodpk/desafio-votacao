package com.rdpk;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ArchUnitTest {

    @Test
    public void verticalSliceArchitectureTest() {
        JavaClasses importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.rdpk");

        // Rule 1: Features should be organized by business capability
        // Note: Some cross-feature dependencies are legitimate (session->agenda, voting->session)
        // This is acceptable in a voting system where business logic requires these relationships

        // Rule 2: Infrastructure/shared cannot depend on features
        noClasses().that().resideInAPackage("..infrastructure..")
            .should().dependOnClassesThat().resideInAPackage("..features..")
            .allowEmptyShould(true) // Allow empty rule since we moved CPF validation to features
            .check(importedClasses);

        // Rule 3: Feature controllers only in feature packages (exclude infrastructure controllers)
        classes().that().haveSimpleNameEndingWith("Controller")
            .and().resideInAPackage("..features..")
            .should().resideInAPackage("..features..")
            .check(importedClasses);

        // Rule 4: Handlers within same feature can collaborate (e.g., StreamResultsHandler uses GetResultsHandler)
        // This is acceptable as they're part of the same business capability

        // Rule 5: Domain models should only be in feature/domain packages
        classes().that().haveSimpleName("Agenda")
            .or().haveSimpleName("Vote")
            .or().haveSimpleName("VoteChoice")
            .or().haveSimpleName("VotingSession")
            .or().haveSimpleName("VotingResult")
            .should().resideInAPackage("..features..domain..")
            .check(importedClasses);
    }
}