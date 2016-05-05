package ar.edu.famaf.nlp.alusivo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * dbPedia priorities, from Pacheco et al. (2012)
 * 
 * @author Pablo Duboue <pablo.duboue@gmail.com>
 * 
 */

// Reference:

// @inproceedings{pacheco2012feasibility,
// title={On the feasibility of open domain referring expression generation
// using large scale folksonomies},
// author={Pacheco, Fabi{\'a}n and Duboue, Pablo Ariel and Dom{\'\i}nguez,
// Mart{\'\i}n Ariel},
// booktitle={Proceedings of the 2012 conference of the north american chapter
// of the association for computational linguistics: Human language
// technologies},
// pages={641--645},
// year={2012},
// organization={Association for Computational Linguistics}
// }

public class TypePriorities {

    public static String dbPediaCityPriorities[] = { "locationCity-1", "city-1", "location-1", "headquarter-1",
            "residence-1", "deathPlace-1", "birthPlace-1", "country", "capital-1", };
    public static String dbPediaCountryPriorities[] = { "capital", "location", "leaderName", "birthPlace-1",
            "location-1", "nationality-1", "country-1", "ethnicGroup" };
    public static Map<String, List<String>> dbPediaIgnored = new HashMap<String, List<String>>();
    public static String[] dbPediaOrganizationIgnored = new String[] { "nick", "wgs84_pos#lat", "wgs84_pos#long" };
    public static String dbPediaOrganizationPriorities[] = { "country", "ethnicGroup", "country-1", "nationality-1",
            "location-1", "birthPlace-1", "leaderName", "location", "capital", "notableCommander",
            "activeYearsStartYear", "battle", "identificationSymbol", "color", "activeYearsEndYear", "role",
            "narrator", "producer", "runtime", "director", "ethnicity-1", "affiliation-1", "knownFor-1", "battle-1",
            "spokenIn-1", "related-1", "foundingYear", "currency", "related", "totalPopulation",
            "owningOrganisation-1", "dissolutionDate", "operator-1", "type-1", "commandStructure-1", "dissolutionYear",
            "owningCompany-1", "militaryBranch-1", "region-1", "parentOrganisation-1", "foundingDate", "owner-1",
            "locationCountry-1", "populationPlace", "point", "place-1", "22-rdf-syntax-ns#type", "hometown-1",
            "publisher-1", "employer-1", "regionServed-1", "language", "usingCountry-1", "headquarter-1", "award-1",
            "occupation-1", "ground-1", "city-1", "foundationPerson-1", "broadcastArea-1", "industry-1", "demonym",
            "almaMater-1", "series-1", "foundationPlace", "thirdDriverCountry-1", "builder-1", "homepage",
            "populationPlace-1", "director-1", "riverMouth-1", "populationDensity", "training-1",
            "distributingCompany-1", "recordedIn-1", "language-1", "channel-1", "militaryUnitSize", "genre-1",
            "firstAscentPerson-1", "distributor-1", "origin-1", "movement-1", "designer-1", "area-1", "formationYear",
            "percentageOfAreaWater", "garrison", "areaMetro", "deathCause-1", "countryWithFirstAstronaut-1", "team-1",
            "assembly-1", "formationDate", "majorShrine-1", "billed-1", "type", "countryOrigin-1", "headquarters-1",
            "anthem", "party-1", "mouthCountry-1", "recordLabel-1", "twinCountry-1", "distributingLabel-1",
            "restingPlace-1", "binomialAuthority-1", "company-1", "subject-1", "deathPlace-1", "officialLanguage",
            "computingPlatform-1", "governmentType", "computingMedia-1", "network-1", "countryWithFirstSpaceflight-1",
            "sourceCountry-1", "regionalLanguage", "strength-1", "residence-1", "foundationPerson", "format-1",
            "hubAirport-1", "anniversary", "developer-1", "leaderTitle", "foundationPlace-1", "locationCity-1",
            "stateOfOrigin-1", "garrison-1", "locatedInArea-1", "combatant-1", "citizenship-1", "subsidiary-1",
            "subsequentWork-1", "education-1", "wineRegion-1", "meetingBuilding-1", "countryWithFirstSatellite-1",
            "crosses-1", "countryWithFirstSatelliteLaunched-1", "parentCompany-1", "meetingCity-1", "province-1",
            "territory-1", "highschool-1", "state-1", "areaTotal", "largestCity", "league-1", "district-1",
            "elevation", "college-1", "populationTotal", "influencedBy-1", "campus-1", "academicAdvisor",
            "academyAward", "americanComedyAward", "appointer", "area", "associatedAct", "baftaAward", "beatifiedBy",
            "billed", "board", "bodyDiscovered", "canonizedBy", "canonizedBy-1", "canonizedPlace", "chancellor",
            "choreographer", "citizenship", "coach", "coachedTeam", "currentPartner", "currentPartner-1", "deathCause",
            "debutTeam", "doctoralAdvisor", "doctoralStudent", "doctoralStudent-1", "education", "emmyAward", "era",
            "ethnicity", "field", "firstRace", "firstWin", "formerChoreographer", "formerCoach", "formerPartner",
            "foundationPlace", "goldenGlobeAward", "highschool", "incumbent", "industry", "influencedBy-1",
            "keyPerson", "lastRace", "lastWin", "league", "location", "mainInterest", "majorShrine", "mission",
            "movement", "nominee", "notableIdea", "notableStudent", "notableStudent-1", "notableWork", "olivierAward",
            "opponent", "opponent-1", "owner", "owningCompany", "parentCompany", "partner", "partner-1",
            "personFunction", "philosophicalSchool", "placeOfBurial", "product", "prospectTeam", "regionServed",
            "relation-1", "relative", "relative-1", "restingPlace", "restingPlacePosition", "royalAnthem",
            "runningMate", "school", "selection", "significantBuilding", "significantProject", "tonyAward", "trainer",
            "trainer-1", "training", "type", "university", "veneratedIn", "winsAtAsia", "winsAtAus", "winsAtJapan",
            "winsAtMajors", "winsAtOtherTournaments", "winsAtPGA", "winsInEurope", "academicDiscipline", "affiliation",
            "aircraftAttack", "aircraftBomber", "aircraftElectronic", "aircraftFighter", "aircraftHelicopter",
            "aircraftInterceptor", "aircraftPatrol", "aircraftRecon", "aircraftTrainer", "aircraftTransport", "album",
            "alliance", "architect", "architect-1", "architecturalStyle", "artist", "artist-1", "assembly",
            "associatedBand", "associatedBand-1", "associatedMusicalArtist", "associatedMusicalArtist-1",
            "associateEditor", "athletics", "athletics-1", "author", "author-1", "automobilePlatform", "award",
            "bandMember", "bandMember-1", "basedOn", "basedOn-1", "binomialAuthority", "birthPlace", "bodyStyle",
            "border", "broadcastArea", "broadcastNetwork", "broadcastNetwork-1", "builder", "campus", "capital-1",
            "capitalMountain", "capitalPlace", "capitalPosition", "capitalRegion", "ceo", "ceremonialCounty-1",
            "chairman", "chairperson", "channel", "chiefEditor", "child", "childOrganisation", "childOrganisation-1",
            "cinematography", "city", "class", "clubsRecordGoalscorer", "colour", "commander", "commandStructure",
            "company", "composer", "computingInput", "computingMedia", "computingPlatform", "constructionMaterial",
            "county-1", "creativeDirector", "creator", "creator-1", "crosses", "currency-1", "daylightSavingTimeZone",
            "dean", "department", "designCompany", "designer", "destination", "developer", "discoverer",
            "distributingCompany", "distributingLabel", "distributor", "district", "division", "division-1", "editing",
            "editor", "editor-1", "endingTheme", "engine", "engineer", "engineType", "equipment", "ethnicGroup-1",
            "europeanAffiliation", "europeanAffiliation-1", "europeanParliamentGroup", "executiveProducer", "family",
            "federalState", "firstAppearance", "format", "formerBandMember", "formerBandMember-1",
            "formerBroadcastNetwork", "formerBroadcastNetwork-1", "foundedBy", "foundedBy-1", "foundingPerson",
            "fourthCommander", "frazioni", "gameEngine", "generalManager", "genre", "genus", "governingBody",
            "governingBody-1", "governmentCountry", "governmentPosition", "governmentRegion", "grades", "ground",
            "head", "headquarter", "headquarters", "highestPlace", "highestPosition", "highestRegion", "homeStadium",
            "homeStadium-1", "hometown", "honours", "hubAirport", "ideology", "ideology-1", "influenced",
            "influencedBy", "instrument", "instrument-1", "internationalAffiliation", "internationalAffiliation-1",
            "isoCodeRegion", "isPartOf", "isPartOf-1", "jurisdiction", "jurisdiction-1", "keyPerson-1", "kingdom",
            "largestCity-1", "largestSettlement", "largestSettlement-1", "lastAppearance", "launchSite",
            "launchSite-1", "layout", "leader", "leaderFunction", "leaderName-1", "leaderParty", "leaderParty-1",
            "license", "license-1", "literaryGenre", "locatedInArea", "locationCity", "locationCountry", "lounge",
            "lowestMountain", "lowestPlace", "lyrics", "mainOrgan", "maintainedBy", "managementPosition", "manager",
            "manufacturer", "manufacturer-1", "march", "mayor", "mediaType", "memberOfParliament",
            "memberOfParliament-1", "mergedIntoParty", "militaryBranch", "musicalArtist", "musicalArtist-1",
            "musicalBand", "musicalBand-1", "musicBy", "musicComposer", "nationalAffiliation", "nationalAffiliation-1",
            "nationality", "nearestCity", "nearestCity-1", "neighboringMunicipality", "network", "nonFictionSubject",
            "occupation", "openingTheme", "operatedBy", "operatedBy-1", "operatingSystem", "operatingSystem-1",
            "operator", "order", "origin", "owningOrganisation", "parentOrganisation", "part", "part-1", "patron",
            "person", "phylum", "picture", "pictureFormat", "politicalPartyInLegislature-1",
            "politicalPartyOfLeader-1", "portrayer", "powerType", "predecessor", "predecessor-1", "presenter",
            "president", "president-1", "previousEditor", "previousWork", "previousWork-1", "principal", "producer-1",
            "product-1", "programmeFormat", "programmingLanguage", "province", "provost", "publisher",
            "railwayRollingStock", "recordedIn", "recordLabel", "rector", "region", "relatedMeanOfTransportation",
            "relatedMeanOfTransportation-1", "religion", "residence", "resolution", "rival", "routeEnd", "routeEnd-1",
            "routeEndLocation", "routeJunction-1", "routeStart", "routeStart-1", "routeStartLocation", "saint",
            "schoolBoard", "secondCommander", "secretaryGeneral", "series", "service", "service-1",
            "servingRailwayLine", "showJudge", "similar", "sisterNewspaper", "sisterNewspaper-1", "sisterStation",
            "sisterStation-1", "species", "splitFromParty", "spokesperson", "sport", "sport-1", "spouse", "starring",
            "starring-1", "state", "stateOfOrigin", "storyEditor", "structuralSystem", "subsequentWork", "subsidiary",
            "successor", "successor-1", "targetAirport", "targetAirport-1", "team", "tenant", "tenant-1",
            "thirdCommander", "timeZone", "timeZone-1", "translator", "twinCity", "twinCity-1", "twinCountry",
            "typeOfElectrification", "usingCountry", "viceChancellor", "voice", "writer", "youthWing" };
    public static String[] dbPediaPersonIgnored = { "birthDate", "birthName" };

    public static String dbPediaPersonPriorities[] = { "type", "orderInOffice", "nationality", "country", "profession",
            "birthPlace", "leaderName-1", "keyPerson-1", "author-1", "commander-1", "occupation", "knownFor",
            "instrument", "successor", "monarch", "successor-1", "primeMinister-1", "activeYearsEndDate", "party",
            "deathDate", "deathPlace", "child", "almaMater", "activeYearsStartDate", "religion", "spouse",
            "president-1", "notableCommander-1", "vicePresident", "president", "primeMinister", "award",
            "militaryRank", "child-1", "militaryCommand", "serviceStartYear", "office", "battle", "spouse-1",
            "knownFor-1", "predecessor", "foundationPerson-1", "monarch-1", "predecessor-1", "activeYearsStartYear",
            "activeYearsEndYear", "starring-1", "lieutenant", "parent", "governor-1", "homepage", "residence",
            "appointer-1", "subject-1", "parent-1", "occupation-1", "region", "stateOfOrigin", "employer", "genre",
            "hometown", "associatedMusicalArtist", "associatedBand", "governor", "deputy", "vicePresident-1",
            "lieutenant-1", "governorGeneral", "governorGeneral-1", "influenced-1", "influencedBy", "team",
            "managerClub", "influenced", "grammyAward", "statisticLabel", "formerTeam", "otherParty", "associate-1",
            "associate", "recordLabel", "militaryBranch", "militaryUnit", "deputy-1", "beatifiedBy-1",
            "associatedBand-1", "associatedMusicalArtist-1", "relation", "college", "draftTeam", "chancellor-1",
            "incumbent-1" };
    public static Map<String, List<String>> dbPediaPriorities = new HashMap<String, List<String>>();

    static {
        dbPediaPriorities.put("http://dbpedia.org/ontology/Person", Arrays.asList(dbPediaPersonPriorities));
        dbPediaIgnored.put("http://dbpedia.org/ontology/Person", Arrays.asList(dbPediaPersonIgnored));
        dbPediaPriorities.put("http://dbpedia.org/ontology/City", Arrays.asList(dbPediaCityPriorities));
        dbPediaPriorities.put("http://dbpedia.org/ontology/Country", Arrays.asList(dbPediaCountryPriorities));
        dbPediaPriorities.put("http://dbpedia.org/ontology/Organisation", Arrays.asList(dbPediaOrganizationPriorities));
        dbPediaIgnored.put("http://dbpedia.org/ontology/Organisation", Arrays.asList(dbPediaOrganizationIgnored));
    }

    private TypePriorities() {
        // this class cannot be instantiated (only static methods)
    }

}
