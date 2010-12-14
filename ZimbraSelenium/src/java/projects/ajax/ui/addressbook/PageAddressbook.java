package  projects.ajax.ui.addressbook;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;

import projects.ajax.ui.AbsAjaxPage;
import projects.ajax.ui.PageMain;
import framework.core.ClientSessionFactory;
import framework.items.ContactItem;
import framework.ui.AbsApplication;
import framework.ui.AbsSeleniumObject;
import framework.ui.Action;
import framework.ui.Button;
import framework.util.HarnessException;
import framework.util.SleepUtil;

public class PageAddressbook extends AbsAjaxPage{

	
	
	 	
	public PageAddressbook(AbsApplication application) {
		super(application);		
		logger.info("new " + PageAddressbook.class.getCanonicalName());

	}

	/* (non-Javadoc)
	 * @see projects.admin.ui.AbsPage#isActive()
	 */
	@Override
	public boolean zIsActive() throws HarnessException {

		// Make sure the main page is active
	 if ( !this.MyApplication.zPageMain.zIsActive() ) {
			this.MyApplication.zPageMain.zNavigateTo();
		}
    
		//make sure Addressbook  tab is selected		
	    String attrs = sGetAttribute("xpath=(//div[@id='zb__App__Contacts'])@class");		
		
		boolean active=attrs.contains("ZSelected");
		
	    //make sure Addressbook folder is displayed
		String locator = "xpath=//div[@id='ztih__main_Contacts__ADDRBOOK_div']";
		
		active &= this.sIsElementPresent(locator);
			
		return (active);

	}

	/* (non-Javadoc)
	 * @see projects.admin.ui.AbsPage#myPageName()
	 */
	@Override
	public String myPageName() {
		return (this.getClass().getName());
	}

	/* (non-Javadoc)
	 * @see projects.admin.ui.AbsPage#navigateTo()
	 */
	@Override
	public void zNavigateTo() throws HarnessException {

		// Check if this page is already active.
		if ( zIsActive() ) {
			return;
		}
		
	
		if ( !MyApplication.zPageMain.zIsActive() ) {
			MyApplication.zPageMain.zNavigateTo();
		}
	
		// Click on Addressbook icon
		if ( !sIsElementPresent(PageMain.Locators.zAppbarContact) ) {
			throw new HarnessException("Can't locate addressbook icon");
		}
		
		zClick(PageMain.Locators.zAppbarContact);
		
		
		zWaitForActive();

		
		//ClientSessionFactory.session().selenium().click(PageMain.Locators.zAppbarContact);
		//zClick(PageMain.Locators.zAppbarContact);
		//SleepUtil.sleepMedium();
		
	}
	public List<ContactItem> zListGetContacts() throws HarnessException {
		
		List <ContactItem> list= new ArrayList<ContactItem>();
		
		//ensure it is in Addressbook main page
	    zNavigateTo();
	    if ( !this.sIsElementPresent("//div[@id='zv__CNS']") )
	    	//maybe return empty list?????
			throw new HarnessException("Contact List is not present "+ "//div[@id='zv__CNS']");
		
	    //Get the number of contacts (String) 
	    int count = this.sGetXpathCount("//div[@id='zv__CNS']//div[contains(@id, 'zli__CNS__')]");
		logger.debug(myPageName() + " zListGetContacts: number of contacts: "+ count);

		// Get each contact's data from the table list
		for (int i = 1; i <= count; i++) {
         	String contactDisplayedLocator = "//div[@id='zv__CNS']/div["+ i +"]/table/tbody/tr/td[3]";
						
			ContactItem ci=new ContactItem(ClientSessionFactory.session().selenium().getText(contactDisplayedLocator));		    			
			list.add(ci);	    	      
		}
       
		    
		return list;		
	}
	
	@Override
	public AbsSeleniumObject zToolbarPressButton(Button button) throws HarnessException {
		logger.info(myPageName() + " zToolbarPressButton("+ button +")");
		
		if ( button == null )
			throw new HarnessException("Button cannot be null!");
		
				
		// Default behavior variables
		//
		String locator = null;			// If set, this will be clicked
		AbsSeleniumObject page = null;	// If set, this page will be returned
		
	   if ( button == Button.B_NEW ) {			
			// For "NEW" without a specified pulldown option, just return the default item
			// To use "NEW" with a pulldown option, see  zToolbarPressPulldown(Button, Button)
			//
		    MyApplication.zKeyboard.zTypeCharacters("n");
			
			// Not default behavior (zPressKeyboardShortcut vs. zClick).
			// Do not fall through.
			return (new FormContactNew(this.MyApplication));			
	   } else if ( button == Button.B_DELETE ) {

		String id = "zb__CNS__DELETE_left_icon";
		
		// Check if the button is enabled
		String attrs = sGetAttribute("xpath=(//td[@id='"+ id +"']/div)@class");
		if ( attrs.contains("ZDisabledImage") ) {
			throw new HarnessException("Tried clicking on "+ button +" but it was disabled "+ attrs);
		}

		locator = "id="+ id;
	   
	   } else if ( button == Button.B_EDIT ) {

		String id = "zb__CNS__EDIT_left_icon";
		
		// Check if the button is enabled
		String attrs = sGetAttribute("xpath=(//td[@id='"+ id +"']/div)@class");
		if ( attrs.contains("ZDisabledImage") ) {
			throw new HarnessException("Tried clicking on "+ button +" but it was disabled "+ attrs);
		}

		locator = "id="+ id;
		page = new FormContactNew(MyApplication);
	   }
	   
	   
        // Default behavior, process the locator by clicking on it
		//
	   
		// Make sure the button exists
		if ( !this.sIsElementPresent(locator) )
			throw new HarnessException("Button is not present locator="+ locator +" button="+ button);
		
		// Click it
		this.zClick(locator);

		return (page);
	}
	
	@Override
	public AbsSeleniumObject zToolbarPressPulldown(Button pulldown, Button option) throws HarnessException {
		logger.info(myPageName() + " zToolbarPressButtonWithPulldown("+ pulldown +", "+ option +")");
		
		if ( pulldown == null )
			throw new HarnessException("Button cannot be null!");
		
		if ( pulldown == null )
			throw new HarnessException("Button cannot be null!");

		// Default behavior variables
		//
		String pulldownLocator = null;	// If set, this will be expanded
		String optionLocator = null;	// If set, this will be clicked
		AbsSeleniumObject page = null;	// If set, this page will be returned
	
		return page;
	}
	
	@Override
	public AbsSeleniumObject zListItem(Action action, Action option, String subject) throws HarnessException {
		throw new HarnessException("implement me!");
	}
	
	@Override
	public AbsSeleniumObject zListItem(Action action, String contact) throws HarnessException {
		logger.info(myPageName() + " zListItem("+ action +", "+ contact +")");
		
		AbsSeleniumObject page = null;
		
		if ( action == Action.A_LEFTCLICK ) {
			
			String listLocator = "//div[@id='zv__CNS']";
			String rowLocator = "//div[contains(@id, 'zli__CNS__')]";
			
			if ( !this.sIsElementPresent(listLocator) )
				throw new HarnessException("List View Rows is not present "+ listLocator);

			if ( !this.sIsElementPresent(rowLocator) )
				throw new HarnessException("List does not contain any items "+ rowLocator);
			
			 //Get the number of contacts (String) 
		    int count = this.sGetXpathCount(listLocator + rowLocator);
			logger.debug(myPageName() + " zListItem: number of contacts: "+ count);

			if ( count == 0 )
				throw new HarnessException("List count was zero");
			
			// Get each contact's data from the table list
			for (int i = 1; i <= count; i++) {
				
				String itemLocator = listLocator + rowLocator + "[" + i +"]";
				
				if ( !this.sIsElementPresent(itemLocator) ) {
					throw new HarnessException("unable to locate item " + itemLocator);
				}
				
	         	String contactDisplayedLocator = itemLocator + "//td[3]";
	         	String displayAs = this.sGetText(contactDisplayedLocator);
	         	
	         	// Log this item to the debug output
	         	LogManager.getLogger("projects").info("zListItem: found contact "+ displayAs);
	         	
	         	if ( !contact.equals(displayAs) )
	         		continue;
								         	
	         	//click
	         	this.zClick(contactDisplayedLocator);
	         	SleepUtil.sleepSmall();
	         	
	         	// All done
	         	return (new DisplayContact(MyApplication));
	         	
			}
			
			throw new HarnessException("Never found the contact "+ contact);

	       				
		}
		else {
			throw new HarnessException("implement me!");
		}
		
		// return (new DisplayContact(MyApplication));
		
	}


}
