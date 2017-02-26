package reciter.algorithm.evidence.targetauthor.email.strategy;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import reciter.algorithm.evidence.targetauthor.AbstractTargetAuthorStrategy;
import reciter.engine.Feature;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterAuthor;
import reciter.model.identity.Identity;

public class EmailStringMatchStrategy extends AbstractTargetAuthorStrategy {

	private List<String> emailSuffixes;
	
	private final String[] defaultSuffixes = {"@med.cornell.edu", "@mail.med.cornell.edu", "@weill.cornell.edu", "@nyp.org"};
	
	public EmailStringMatchStrategy() {
		setEmailSuffixes(Arrays.asList(defaultSuffixes));
	}
	
	public EmailStringMatchStrategy(List<String> emailSuffixes) {
		this.setEmailSuffixes(emailSuffixes);
	}
	
	@Override
	public double executeStrategy(ReCiterArticle reCiterArticle, Identity identity) {
		double score = 0;
		for (ReCiterAuthor author : reCiterArticle.getArticleCoAuthors().getAuthors()) {
			if (author.getAffiliation() != null && author.getAffiliation() != null) {
				String affiliation = author.getAffiliation();
				
				for (String suffix : emailSuffixes) {
					String email = identity.getUid() + suffix;
					if (StringUtils.containsIgnoreCase(affiliation, email)) {
						reCiterArticle.setClusterInfo(reCiterArticle.getClusterInfo() + " [email matches: " + affiliation + "]");
						reCiterArticle.getMatchingEmails().add(email);
						score += 1;
					}
				}
				
				// TODO output features.

				for (String email : identity.getEmails()) {
					if (affiliation.contains(email)) {
						reCiterArticle.setClusterInfo(reCiterArticle.getClusterInfo() + " [email matches: " + email + "]");
						reCiterArticle.getMatchingEmails().add(email);
						score += 1;
					}
				}
			}
		}
		reCiterArticle.setEmailStrategyScore(score);
		return score;
	}

	@Override
	public double executeStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		double sumScore = 0;
		for (ReCiterArticle reCiterArticle : reCiterArticles) {
			sumScore += executeStrategy(reCiterArticle, identity);
		}
		return sumScore;
	}
	
	/**
	 * @med.cornell.edu", "@mail.med.cornell.edu", "@weill.cornell.edu", "@nyp.org
	 */
	@Override
	public void populateFeature(ReCiterArticle reCiterArticle, Identity identity, Feature feature) {
		for (ReCiterAuthor author : reCiterArticle.getArticleCoAuthors().getAuthors()) {
			if (author.getAffiliation() != null && author.getAffiliation() != null) {
				String affiliation = author.getAffiliation();
				if (StringUtils.containsIgnoreCase(affiliation, identity.getUid() + defaultSuffixes[0])) {
					feature.setMedCornellEdu(1);
				} else if (StringUtils.containsIgnoreCase(affiliation, identity.getUid() + defaultSuffixes[1])) {
					feature.setMailMedCornellEdu(1);
				} else if (StringUtils.containsIgnoreCase(affiliation, identity.getUid() + defaultSuffixes[2])) {
					feature.setWeillCornellEdu(1);
				} else if (StringUtils.containsIgnoreCase(affiliation, identity.getUid() + defaultSuffixes[3])) {
					feature.setNypOrg(1);
				}
			}
		}
	}

	public String[] getDefaultSuffixes() {
		return defaultSuffixes;
	}

	public List<String> getEmailSuffixes() {
		return emailSuffixes;
	}

	public void setEmailSuffixes(List<String> emailSuffixes) {
		this.emailSuffixes = emailSuffixes;
	}
}
