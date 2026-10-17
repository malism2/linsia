import os
import logging
from datetime import datetime
from dotenv import load_dotenv
from supabase import create_client

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(filename)s - %(funcName)s - %(lineno)d - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class DBUtil:
    def __init__(self):
        load_dotenv()
        self.supabase = create_client(os.getenv("SUPABASE_URL"), os.getenv("SUPABASE_KEY"))

    def get_data(self, table_name, filters):
        query = self.supabase.table(table_name).select("*")
        for k, v in filters.items():
            query.eq(k, v)
        return query.execute()
    
    def insert_data(self, table_name, data):
        return self.supabase.table(table_name).insert(data).execute()
    
    def update_data(self, table_name, data, filters):
        query = self.supabase.table(table_name).update(data)
        for k, v in filters.items():
            query.eq(k, v)
        return query.execute()

    def rm_data(self, table_name, filters):
        query = self.supabase.table(table_name).delete()
        for k, v in filters.items():
                query.eq(k, v)
        return query.execute()

    def save_navigation(self, data):
        key = 'name'
        name = data[key]
        logger.info(f"正在处理数据: {name}")
        old = self.get_data("web_navigation", {key: name})
        id = 0
        if old.data:
            id = old.data[0]['id']
            
        logger.info(f"查询数据结果: {id}")
        value = {
            'name': name,
            'url': data['url'],
            'title': data['summary']['en']['title'],
            'content': data['summary']['en']['description'],
            'detail': data['summary']['en']['detail'],
            'image_url': data['screenshot'],
            'thumbnail_url': data['screenshot_thumbnail'],
            'tag_name': None,
            'website_data': None,
            'collection_time': datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S'),
            'star_rating': 0,
            'category_name': ''
        }
        if old.data:
            res = self.update_data("web_navigation", value, {key: name})
            id = res.data[0]['id']
            logger.info(f"更新数据成功: {res.data[0]['id']}")
        else:
            res = self.insert_data("web_navigation", value)
            id = res.data[0]['id']
            logger.info(f"插入数据成功: {res.data[0]['id']}")
        for k, v in data['summary'].items():
            if k == 'en':
                continue
            i = {
                'name': name,
                'locale': k,
                'title': v['title'],
                'content': v['description'],
                'detail': v['detail'],
            }
            self.save_save_navigation_i18n(name, k, i)

        return id        
        
    def save_save_navigation_i18n(self, name, locale, value):
        f = {'name': name, 'locale': locale}
        old = self.get_data("web_navigation_i18n", f)
        id = 0
        if old.data:
            res = self.update_data("web_navigation_i18n", value, f)
            id = res.data[0]['id']
            logger.info(f"更新i18n成功: {name},{locale},{res.data[0]['id']}")
        else:
            res = self.insert_data("web_navigation_i18n", value)
            id = res.data[0]['id']
            logger.info(f"插入i18n数据成功: {name},{locale},{res.data[0]['id']}")
        return id        
