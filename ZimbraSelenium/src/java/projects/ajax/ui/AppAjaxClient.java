/**
 * 
 */
package projects.ajax.ui;

import projects.ajax.core.AjaxCommonTest;
import projects.ajax.ui.addressbook.PageAddressbook;
import projects.ajax.ui.addressbook.TreeContacts;
import projects.ajax.ui.briefcase.PageBriefcase;
import projects.ajax.ui.mail.PageMail;
import projects.ajax.ui.preferences.PagePreferences;
import projects.ajax.ui.preferences.TreePreferences;
import projects.ajax.ui.search.PageSearch;
import framework.ui.AbsApplication;
import framework.ui.Keyboard;
import framework.util.HarnessException;
import framework.util.ZimbraAccount;

/**
 * The <code>AppAjaxClient</code> class defines the Zimbra Ajax client.
 * <p>
 * The <code>AppAjaxClient</code> contains all pages, folder trees,
 * dialog boxes, forms, menus for the Ajax client.
 * <p>
 * In {@link AjaxCommonTest}, there is one
 * AppAjaxClient object created per test case class (ensuring 
 * class-level concurrency).  The test case methods can access
 * different application pages and trees, using the object
 * properties.
 * <p>
 * <pre>
 * {@code
 * 
 * // Navigate to the addresbook
 * app.zPageAddressbook.navigateTo();
 * 
 * // Click "New" button to create a new contact
 * app.zPageAddressbook.zToolbarPressButton(Button.B_NEW);
 * 
 * }
 * </pre>
 * <p>
 * 
 * @author Matt Rhoades
 *
 */
public class AppAjaxClient extends AbsApplication {
	
	public Keyboard						zKeyboard = null;
	public PageLogin					zPageLogin = null;
	public PageMain						zPageMain = null;
	public PageSearch					zPageSearch = null;
	public PageMail						zPageMail = null;
	public PageBriefcase                zPageBriefcase = null;
	public PageAddressbook              zPageAddressbook = null;
	public PagePreferences				zPagePreferences = null;
	
	public TreeContacts					zTreeContacts = null;
	public TreePreferences				zTreePreferences = null;
	
	public AppAjaxClient() {
		super();
		
		logger.info("new " + AppAjaxClient.class.getCanonicalName());
		
		// Keyboard pointer
		zKeyboard = new Keyboard(this);
		
		// Login page
		
		zPageLogin = new PageLogin(this);
		pages.put(zPageLogin.myPageName(), zPageLogin);
		
		// Main page
		zPageMain = new PageMain(this);
		pages.put(zPageMain.myPageName(), zPageMain);
		
		zPageSearch = new PageSearch(this);
		pages.put(zPageSearch.myPageName(), zPageSearch);
		
		// Mail page
		zPageMail = new PageMail(this);
		pages.put(zPageMail.myPageName(), zPageMail);
		
		//Addressbook page    
		zPageAddressbook = new PageAddressbook(this);
		pages.put(zPageAddressbook.myPageName(), zPageAddressbook);

		zTreeContacts = new TreeContacts(this);
		trees.put(zTreeContacts.myPageName(), zTreeContacts);
		
		// PageBriefcase page
		zPageBriefcase = new PageBriefcase(this);
		pages.put(zPageBriefcase.myPageName(), zPageBriefcase);
		
		// Preferences page
		zPagePreferences = new PagePreferences(this);
		pages.put(zPagePreferences.myPageName(), zPagePreferences);

		zTreePreferences = new TreePreferences(this);
		trees.put(zTreePreferences.myPageName(), zTreePreferences);
		

	}
	
	/* (non-Javadoc)
	 * @see projects.admin.ui.AbsApplication#isLoaded()
	 */
	@Override
	public boolean zIsLoaded() throws HarnessException {
		// TODO: Need to define this method
		return (true);
	}

	/* (non-Javadoc)
	 * @see projects.admin.ui.AbsApplication#myApplicationName()
	 */
	@Override
	public String myApplicationName() {
		return ("Ajax Client");
	}

	/* (non-Javadoc)
	 * @see projects.admin.ui.AbsApplication#myApplicationName()
	 */
	@Override
	protected ZimbraAccount zSetActiveAcount(ZimbraAccount account) throws HarnessException {
		return (super.zSetActiveAcount(account));
	}

}
