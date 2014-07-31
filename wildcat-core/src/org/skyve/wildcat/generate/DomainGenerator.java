package org.skyve.wildcat.generate;

import java.util.Map.Entry;

import org.skyve.metadata.customer.Customer;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.module.Module;
import org.skyve.metadata.module.Module.DocumentRef;
import org.skyve.metadata.view.View;
import org.skyve.metadata.view.View.ViewType;
import org.skyve.wildcat.metadata.repository.AbstractRepository;
import org.skyve.wildcat.metadata.repository.LocalDesignRepository;
import org.skyve.wildcat.metadata.repository.router.UxUi;
import org.skyve.wildcat.util.UtilImpl;

public abstract class DomainGenerator {
	protected static final String DECIMAL2 = "Decimal2";
	protected static final String DECIMAL5 = "Decimal5";
	protected static final String DECIMAL10 = "Decimal10";
	protected static final String DATE_ONLY = "DateOnly";
	protected static final String DATE_TIME = "DateTime";
	protected static final String TIME_ONLY = "TimeOnly";
	protected static final String TIMESTAMP = "Timestamp";
	protected static final String GEOMETRY = "Geometry";
	
	protected static String SRC_PATH; 

	public static final void validate(String customerName) throws Exception {
		AbstractRepository repository = AbstractRepository.get();

		System.out.println("Get customer " + customerName);
		Customer customer = repository.getCustomer(customerName);
		System.out.println("Validate customer " + customerName);
		repository.validateCustomer(customer);
		for (Module module : customer.getModules()) {
			System.out.println("Validate module " + module.getName());
			repository.validateModule(customer, module);
			for (Entry<String, DocumentRef> entry : module.getDocumentRefs().entrySet()) {
				String documentName = entry.getKey();
				System.out.println("Get document " + documentName);
				Document document = module.getDocument(customer, documentName);
				System.out.println("Validate document " + documentName);
				repository.validateDocument(customer, document);
				for (UxUi uxui : repository.getRouter().getUxUis()) {
					String uxuiName = uxui.getName();
					System.out.println("Get edit view for document " + documentName + " and uxui " + uxuiName);
					View view = repository.getView(uxuiName, customer, document, ViewType.edit);
					System.out.println("Validate edit view for document " + documentName + " and uxui " + uxuiName);
					repository.validateView(customer, document, view, uxuiName);
					view = repository.getView(uxuiName, customer, document, ViewType.create);
					if (view != null) {
						System.out.println("Validate create view for document " + documentName + " and uxui " + uxuiName);
						repository.validateView(customer, document, view, uxuiName);
					}
				}
			}
		}
	}

	public abstract void generate() throws Exception;

	public static void main(String[] args) throws Exception {
		if (args.length >= 1) {
			SRC_PATH = args[0];
		}
		else {
			System.err.println("You must have at least the src path as an argument - usually \"src/\"");
			System.exit(1);
		}

		if (args.length != 2) { // allow for debug mode if there are 2 arguments
			UtilImpl.CLASS_LOADER_TRACE = false;
			UtilImpl.COMMAND_TRACE = false;
			UtilImpl.CONTENT_TRACE = false;
			UtilImpl.HTTP_TRACE = false;
			UtilImpl.QUERY_TRACE = false;
			UtilImpl.RENDER_TRACE = false;
			UtilImpl.SECURITY_TRACE = false;
			UtilImpl.BIZLET_TRACE = false;
			UtilImpl.SQL_TRACE = false;
			UtilImpl.XML_TRACE = false;
		}
		else {
			UtilImpl.CLASS_LOADER_TRACE = true;
			UtilImpl.COMMAND_TRACE = true;
			UtilImpl.CONTENT_TRACE = true;
			UtilImpl.HTTP_TRACE = true;
			UtilImpl.QUERY_TRACE = true;
			UtilImpl.RENDER_TRACE = true;
			UtilImpl.SECURITY_TRACE = true;
			UtilImpl.BIZLET_TRACE = true;
			UtilImpl.SQL_TRACE = true;
			UtilImpl.XML_TRACE = true;
		}
		
		DomainGenerator foo = UtilImpl.USING_JPA ? new JPADomainGenerator() : new OverridableDomainGenerator();
		AbstractRepository repository = new LocalDesignRepository();
		AbstractRepository.set(repository);

		// generate for all customers
		for (String customerName : repository.getAllCustomerNames()) {
			validate(customerName);
		}

		foo.generate();
	}
}