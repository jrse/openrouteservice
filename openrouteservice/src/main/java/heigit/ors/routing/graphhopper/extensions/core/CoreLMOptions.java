package heigit.ors.routing.graphhopper.extensions.core;

import com.graphhopper.storage.GraphHopperStorage;
import heigit.ors.routing.AvoidFeatureFlags;
import heigit.ors.routing.graphhopper.extensions.edgefilters.core.AvoidBordersCoreEdgeFilter;
import heigit.ors.routing.graphhopper.extensions.edgefilters.core.AvoidFeaturesCoreEdgeFilter;
import heigit.ors.routing.graphhopper.extensions.edgefilters.core.LMEdgeFilterSequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CoreLMOptions {
    List<String> coreLMSets;
    List<LMEdgeFilterSequence> filters = new ArrayList<>();

    /**
     * Set the filters that are used while calculating landmarks and their distance
     * @param tmpCoreLMSets
     */
    public void setRestrictionFilters(List<String> tmpCoreLMSets) {
        this.coreLMSets = tmpCoreLMSets;
    }

    /**
     * Creates all LMSet-Filters from the sets specified in the app.config
     * The filter is an LMEdgeFilterSequence, consisting of at most ONE AvoidFeaturesFilter and ONE AvoidCountriesFilter
     * These can contain multiple avoidfeatures and avoidcountries
     *
     *
     * */
    public void createRestrictionFilters(GraphHopperStorage ghStorage){
        //Create one edgefiltersequence for each lmset
        for(String set : coreLMSets) {
            //Now iterate over all comma separated values in one lm set
            List<String> filters = Arrays.asList(set.split(","));
            LMEdgeFilterSequence edgeFilterSequence = new LMEdgeFilterSequence();
            int feature, country, avoidFeatures = 0;
            List<Integer> countries = new ArrayList<Integer>();

            for (String filterType : filters) {

                //Do not add any filter if it is allow_all
                if (filterType.equalsIgnoreCase("allow_all")) {
                    edgeFilterSequence.appendName("allow_all");
                    break;
                }

                feature = AvoidFeatureFlags.getFromString(filterType);

                //process avoid features
                if (feature != 0) {
                    avoidFeatures = avoidFeatures | feature;
                    edgeFilterSequence.appendName(filterType.toLowerCase());
                    continue;
                }

                //process avoid countries
                String countryPattern = "(?i)country_(\\d+)";

                if (filterType.matches(countryPattern)) {
                    try {
                        country = Integer.parseInt(filterType.replaceFirst(countryPattern, "$1"));
                    }
                    catch (NumberFormatException e)
                    {
                        country = 0;
                    }
                    // todo check for valid country
                    if (country != 0 && !countries.contains(country)) {
                        countries.add(country);
                        edgeFilterSequence.appendName(filterType.toLowerCase());
                    }
                    continue;
                }
            }

            if(avoidFeatures != 0)
                edgeFilterSequence.add(new AvoidFeaturesCoreEdgeFilter(ghStorage, -1, avoidFeatures));

            if(!countries.isEmpty()){
                int[] avoidCountries = new int[countries.size()];
                for(int i = 0; i < countries.size(); i++){
                    avoidCountries[i] = countries.get(i);
                }
                //Only one avoidBordersCoreEdgeFilter per set
                edgeFilterSequence.add(new AvoidBordersCoreEdgeFilter(ghStorage, avoidCountries));
            }

            if(edgeFilterSequence != null)
                this.filters.add(edgeFilterSequence);
        }
    }

    public List<LMEdgeFilterSequence> getFilters(){
        return filters;
    }

}
