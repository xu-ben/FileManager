/*
 * 类名：		FileSearch
 * 创建日期：	2009/10/15
 * 最近修改：	2013/02/22
 * 作者：		徐犇、梅本金
 */

package filesearch;


import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

/**
 * 
 * 文件搜索管理类
 * @author ben
 */
@SuppressWarnings("serial")
public class FileSearch extends JDialog implements MouseListener {
	/**
	 * 搜索按钮
	 */
	private JButton buttonSearch = new JButton("搜索文件类型");
	
	/**
	 * 选择当前路径的按钮
	 */
	private JButton buttonScan = new JButton("更换目录...");
	
	/**
	 * 标签，显示在程序的最下方，提示一些当前状态的信息
	 */
	private JLabel tipLabel = new JLabel("当前目录：");
	
	/**
	 * 用于显示当前选择路径的文本域
	 */
	private JTextField textFilePath = new JTextField("所有本地磁盘");
	
	/**
	 * 可添加链接的面板
	 */
	private JTextPane searchResults = new JTextPane();
	
	private Container con;
	
	/**
	 * 文件类型搜索过程中用来存储和处理文件名的结构
	 */
	private TreeMap<String, Integer> suffixmap = null;

	/**
	 * 用于存放添加到界面的按钮
	 */
	private ArrayList<JButton> buttonList = new ArrayList<JButton>();

	/**
	 * 文件数量
	 */
	public int fileNum;
	
	/**
	 * windows资源管理器路径
	 */
	private String explorePath;
	
	/**
	 * 文件名是否区分大小写的标志
	 */
//	private boolean caseSensitive = false;
	
	/**
	 * 标签，显示在程序的最下方，提示一些当前状态的信息
	 */
	private JLabel stateLabel = new JLabel();
	
	/**
	 * 文件名是否区分大小写的标记
	 */
	private JCheckBox caseSensitive = new JCheckBox();
	
	/**
	 * 构造对话框时调用的初始化函数
	 */
	private void init() {
		buttonScan.addMouseListener(this);
		buttonSearch.addMouseListener(this);

		searchResults.setEditable(false);		
		textFilePath.setFont(new Font("", Font.BOLD, 15));
		searchResults.setFont(new Font("", Font.BOLD, 5));
		caseSensitive.setSelected(false);
		caseSensitive.setText("文件名区分大小写");
		caseSensitive.setBackground(Color.WHITE);
		
		explorePath = getExplorerPath();
		
		/*
		 * 图形界面面板
		 */
		con = this.getContentPane();
		con.setLayout(new BorderLayout(5, 0));
		/*
		 * 面板北部，分成左右两格
		 */
		JPanel north = new JPanel();
		north.setLayout(new GridLayout(1, 2));
		//北部左边
		JPanel northleft = new JPanel();
		northleft.setLayout(new BorderLayout(5, 0));
		northleft.add(tipLabel, BorderLayout.WEST);
		northleft.add(textFilePath, BorderLayout.CENTER);
		textFilePath.setEditable(false);
		textFilePath.setBackground(Color.WHITE);
		northleft.add(buttonScan, BorderLayout.EAST);
		north.add(northleft);
		//北部右边
		JPanel northright = new JPanel();
		northright.setLayout(new GridLayout(1, 2));
		JPanel tmppanel = new JPanel();
		tmppanel.setBackground(Color.WHITE);
		tmppanel.add(caseSensitive);
		northright.add(tmppanel);
		northright.add(buttonSearch);
		north.add(northright);
		con.add(north, BorderLayout.NORTH);
		/*
		 * 面板中部
		 */
		con.add(new JScrollPane(searchResults), BorderLayout.CENTER);
		/*
		 * 面板南部
		 */
		JPanel south = new JPanel();
		south.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		south.add(stateLabel);
		stateLabel.setText("指定当前路径，然后点击\"搜索文件类型\"。");
		con.add(south, BorderLayout.SOUTH);

		/*
		 * 得到屏幕尺寸
		 */
		final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize(); 
		final int width = 1000;
		final int height = 618;
		final int left = (screen.width - width) / 2;
		final int top = (screen.height - height) / 2;
		this.setLocation(left, top);
		this.setSize(width, height);
		this.setResizable(true);
		this.setVisible(true);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);		
	}
	
	/**
	 *  指定所有者 JFrame、模式和标题的构造函数
	 */
	private FileSearch(JFrame owner, String title, boolean modal) {
		super(owner, title, modal);
		init();
	}
	
	/**
	 * 响应选择当前路径按钮的函数
	 */
	private void clickButtonScan() {
		/*
		 * 用JFileChooser设置文件打开对话框，由于只要选择文件夹，
		 * 设置文件选中的模式为目录即JFileChooser.DIRECTORIES_ONLY
		 */
		String filePath = new String();
		try {
			JFileChooser fileChooser = new JFileChooser(".");
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int n = fileChooser.showOpenDialog(this);
			if (n == JFileChooser.APPROVE_OPTION) {
				filePath = fileChooser.getSelectedFile().getPath();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		textFilePath.setText(filePath);		
	}
	
	/**
	 * 搜索文件类型的消息处理函数
	 */
	private void clickButtonSearch() {
		/*
		 * 初始化文件名的存储结构
		 */
		suffixmap = new TreeMap<String, Integer>();
		/*
		 * 清空JButton
		 */
		buttonList.clear();
		/*
		 * 由于当JTextPane的属性设置为只读时，就无法删除前一次搜索
		 * 时显示在JTextPane中的内容，因此先将JTextPane设置为可编辑
		 * 然后删除JTextPane中的内容，这样就可以显示本次搜索结果
		 */
		searchResults.setEditable(true);
		searchResults.selectAll();
		searchResults.replaceSelection("");
//		searchResults.setLayout(new GridLayout(buttonList.size(), 1));
//		this.searchResults.setLayout(new GridLayout(buttonList.size(), 1));
		searchResults.setEditable(false);
		
		/*
		 * 查看当前选择的路径，如果是默认未选择，则搜索本机所有磁盘
		 */
		String curpath = textFilePath.getText().trim();
		File[] fs = null;
		if(curpath.equals("所有本地磁盘")) {
			fs = File.listRoots();
		}else {
			fs = new File[1];
			fs[0] = new File(curpath);
		}
		fileNum = 0;
		for(int i = 0; i < fs.length; i++) {
			fileNum += searchFileTypes(fs[i]);
		}

		Iterator<String>  suffixname = suffixmap.keySet().iterator();
		while(suffixname.hasNext()) {
			String sname = suffixname.next();
			int snum = suffixmap.get(sname);
			JButton bu = new JButton(sname + "(" + snum + ")");
			bu.setToolTipText("点击列出本目录下所有后缀为" + sname + "的文件。");
			buttonList.add(bu);
		}		
		
		/*
		 * 将搜索到的所有文件的路径作为JButton的Text，并为每个JButton 注册监听器
		 */
		for (int i = 0; i < buttonList.size(); i++) {
			JButton bu = buttonList.get(i);
			bu.setSize(100, 100);
			searchResults.insertComponent(bu);
			bu.addMouseListener(this);
		}
		JOptionPane.showMessageDialog(this, "搜索完毕。共发现文件" + fileNum + "个，发现文件类型" + (buttonList.size() - 1) + "种。");
	}
	
	/**
	 * 点击文件类型按钮的消息处理函数
	 */
	private void clickFileType(String text) {
		/*
		 * 清空JButton
		 */
		buttonList.clear();
		/*
		 * 查看当前选择的路径，如果是默认未选择，则搜索本机所有磁盘
		 */
		String curpath = textFilePath.getText().trim();
		File[] fs = null;
		if(curpath.equals("所有本地磁盘")) {
			fs = File.listRoots();
		}else {
			fs = new File[1];
			fs[0] = new File(curpath);
		}
		for(int i = 0; i < fs.length; i++) {
			searchFiles(fs[i], text);
		}

		searchResults.setEditable(true);
		searchResults.selectAll();
		searchResults.replaceSelection("");
		searchResults.setEditable(false);
		
		/*
		 * 将搜索到的所有文件的路径作为JButton的Text，并为每个JButton 注册监听器
		 */
		for (int i = 0; i < buttonList.size(); i++) {
			JButton bu = buttonList.get(i);
			bu.setSize(100, 100);
			searchResults.insertComponent(bu);
			bu.addMouseListener(this);
		}		
	}
	
	/**
	 * 搜索到文件后处理并存储文件名信息的函数
	 * @param f
	 */
	private void recordType(File f) {
		String tmp = f.getName();
		int index = tmp.lastIndexOf('.');
		if(index >= 0) {
			tmp = tmp.substring(index);			
		}else {
			tmp = "nosuffix";
		}
		if(!caseSensitive.isSelected()) {
			tmp = tmp.toLowerCase();
		}
		if (suffixmap.containsKey(tmp)) {
			suffixmap.put(tmp, suffixmap.get(tmp) + 1);
		} else {
			suffixmap.put(tmp, 1);
		}
	}
	
	/**
	 * 递归查找指定目录中相应后缀的文件并将其显示
	 * @param f 指定的目录
	 * @param suffix 指定的文件扩展名(包括.)
	 * @return 符合条件的文件数量
	 */
	public int searchFiles(File f, String suffix) {
		int ret = 0;
		String[] list = f.list();
		if (list == null)
			return ret;
		for (int i = 0; i < list.length; i++) {
			//分隔符写成"/"同时兼容Linux和Windows
			String path = f.getPath() + "/" + list[i];
			File fChild = new File(path);
			if(fChild.isFile() && list[i].endsWith(suffix)) {
				ret++;
				JButton bu = new JButton(list[i]);
				bu.setToolTipText(fChild.getAbsolutePath());
				buttonList.add(bu);
			}else if(fChild.isDirectory()) {
				ret += searchFiles(fChild, suffix);
			}
		}
		return ret;
	}

	/**
	 * 递归搜索文件，得到文件类型及相应文件数量
	 * @param f
	 * @return 找到的文件(不包括目录)总数量
	 */
	public int searchFileTypes(File f) {
		int ret = 0;
		String[] list = f.list();
		if (list == null)
			return ret;
		for (int i = 0; i < list.length; i++) {
			//分隔符写成"/"同时兼容Linux和Windows
			File fChild = new File(f.getPath() + "/" + list[i]);
			if(fChild.isFile()) {
				ret++;
				recordType(fChild);
			}else if(fChild.isDirectory()) {
				ret += searchFileTypes(fChild);
			}
		}
		return ret;
	}
		 
	/**
	 * 获得资源管理器所在的路径
	 * @return 路径字符串
	 */
	public static String getExplorerPath() {
		File[] files = File.listRoots();
		String explore = "windows/explorer.exe";
		for (int i = 1; i < files.length; i++) {
			String pf = files[i].getAbsolutePath();
			File f = new File(pf + explore);
			if (f.exists()) {
//				System.out.println(pf + explore);
				return pf + explore;
			}
		}
		return "";
	}
	
	@Override
	public void mouseClicked(MouseEvent ae) {
		if (ae.getSource() == buttonScan) {//选择当前路径
			clickButtonScan();
		}else if(ae.getSource() == buttonSearch) {
			/*
			 * 耗时操作，显示等待光标
			 */
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			clickButtonSearch();
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}else {
			try {
				JButton bu = (JButton) ae.getSource();
				String text = bu.getText();
				if(text.indexOf('(') >= 0) {//选中的是文件类型按钮
					text = text.substring(0, text.indexOf('('));
					clickFileType(text);
				}else {//选中的是文件名按钮
					File f = new File(bu.getToolTipText());
					if((ae.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {//左键
						if(ae.getClickCount() == 2) {//双击打开文件
							String command = this.explorePath + " " + f.getAbsolutePath();
							Runtime.getRuntime().exec(command);
						}						
					}else if((ae.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {//右键
						String command = this.explorePath + " " + f.getParent();
						Runtime.getRuntime().exec(command);						
					}
				}
			}catch(Exception ex) {
			}
		}
		
	}

	/**
	 * 程序入口
	 * @param args
	 */
	public static void main(String[] args) {
		new FileSearch(null, "Windows文件搜索对话框", false);
	}
	
	/**
	 * 显示一个文件搜索对话框
	 * @param owner
	 * @param title
	 */
	public synchronized static void showFileSearchDialog(JFrame owner, String title) {
		if(title == null || owner == null) {
			return ;
		}
		new FileSearch(owner, title, true);
	}

	@Override
	public void mouseEntered(MouseEvent me) {}
	@Override
	public void mouseExited(MouseEvent arg0) {}
	@Override
	public void mousePressed(MouseEvent arg0) {}
	@Override
	public void mouseReleased(MouseEvent arg0) {}
}
