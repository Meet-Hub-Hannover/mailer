package de.meethub.mailer;

import com.ecwid.mailchimp.MailChimpAPIVersion;
import com.ecwid.mailchimp.MailChimpMethod;
import com.ecwid.mailchimp.MailChimpObject;
import com.ecwid.mailchimp.method.v1_3.campaign.CampaingRelatedMethod;

@MailChimpMethod.Method(name = "campaignSchedule", version = MailChimpAPIVersion.v1_3)
public class CampaignScheduleMethod extends CampaingRelatedMethod<Boolean> {

    @MailChimpObject.Field
    public String schedule_time;

}
