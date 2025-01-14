package knife;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import burp.BurpExtender;
import api.Getter;
import burp.IBurpExtenderCallbacks;
import burp.IContextMenuInvocation;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;


public class UpdateHeaderMenu extends JMenu {
	//JMenuItem vs. JMenu
	//JMenu 可以有子菜单，即使具体实现为空，也会显示
	//JMenuItem 不可以有子菜单，当实现为空的时候，是不会显示的
	public BurpExtender burp;
	public IContextMenuInvocation invocation;
	public UpdateHeaderMenu(BurpExtender burp){

		try {
			this.invocation = burp.invocation;
			this.burp = burp;

			if (invocation.getInvocationContext() == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_REQUEST) {

				List<String> pHeaders = possibleHeaderNames(invocation);//HeaderNames without case change
				/*menu_list.add(uhmenu);*/
				if(!pHeaders.isEmpty()) {
					this.setText("^_^ Update Header");
					for (String pheader:pHeaders) {
						JMenuItem headerItem = new JMenuItem(pheader);
						headerItem.addActionListener(new UpdateHeader_Action(burp,invocation,pheader));
						this.add(headerItem);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(BurpExtender.getStderr());
		}
	}

	public List<String> possibleHeaderNames(IContextMenuInvocation invocation) {
		IHttpRequestResponse[] selectedItems = invocation.getSelectedMessages();
		//byte selectedInvocationContext = invocation.getInvocationContext();
		Getter getter = new Getter(burp.callbacks.getHelpers());
		LinkedHashMap<String, String> headers = getter.getHeaderMap(true, selectedItems[0]);

		String tokenHeadersStr = burp.tableModel.getConfigValueByKey("tokenHeaders");

		List<String> ResultHeaders = new ArrayList<String>();
		
		if (tokenHeadersStr!= null && headers != null) {
			String[] tokenHeaders = tokenHeadersStr.split(",");
			List<String> keywords = Arrays.asList(tokenHeaders);
			Iterator<String> it = headers.keySet().iterator();
			while (it.hasNext()) {
				String item = it.next();
				if (containOneOfKeywords(item,keywords,false)) {
					ResultHeaders.add(item);
				}
			}
		}

		return ResultHeaders;
	}

	public boolean containOneOfKeywords(String x,List<String> keywords,boolean isCaseSensitive) {
		for (String keyword:keywords) {
			if (isCaseSensitive == false) {
				x = x.toLowerCase();
				keyword = keyword.toLowerCase();
			}
			if (x.contains(keyword)){
				return true;
			}
		}
		return false;
	}
}

class UpdateHeader_Action implements ActionListener{
	private IContextMenuInvocation invocation;
	public IExtensionHelpers helpers;
	public PrintWriter stdout;
	public PrintWriter stderr;
	public IBurpExtenderCallbacks callbacks;

	private String headerName;

	public UpdateHeader_Action(BurpExtender burp,IContextMenuInvocation invocation,String headerName) {
		this.invocation  = invocation;
		this.helpers = burp.helpers;
		this.callbacks = burp.callbacks;
		this.stderr = burp.stderr;
		this.stdout = burp.stdout;
		this.headerName = headerName;
	}

	@Override
	public void actionPerformed(ActionEvent event) {

		IHttpRequestResponse[] selectedItems = invocation.getSelectedMessages();
		IHttpRequestResponse messageInfo = selectedItems[0];
		Getter getter = new Getter(BurpExtender.callbacks.getHelpers());
		String shorturl = getter.getShortURL(messageInfo).toString();//current
		HeaderEntry urlAndtoken = CookieUtils.getLatestHeaderFromHistory(shorturl,headerName);

		if (urlAndtoken !=null) {
			LinkedHashMap<String, String> headers = getter.getHeaderMap(true,messageInfo);
			byte[] body = getter.getBody(true,messageInfo);

			headers.put(headerName,urlAndtoken.getHeaderValue());
			List<String> headerList = getter.headerMapToHeaderList(headers);

			byte[] newRequestBytes = BurpExtender.callbacks.getHelpers().buildHttpMessage(headerList, body);
			selectedItems[0].setRequest(newRequestBytes);
		}
	}

}
