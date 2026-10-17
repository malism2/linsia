import os
import time
from dotenv import load_dotenv
import logging
from io import BytesIO
import requests
from botocore.client import Config
from botocore.exceptions import ClientError
import boto3
from PIL import Image

# 设置日志记录
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(filename)s - %(funcName)s - %(lineno)d - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class OSSUtil:
    def __init__(self):
        # Prefer dist/.env when tools are run from repo root.
        util_dir = os.path.dirname(os.path.abspath(__file__))
        dist_dir = os.path.dirname(util_dir)
        load_dotenv(os.path.join(dist_dir, ".env"))
        load_dotenv()
        self.S3_ENDPOINT_URL = os.getenv('S3_ENDPOINT_URL')
        self.S3_ACCESS_KEY_ID = os.getenv('S3_ACCESS_KEY_ID')
        self.S3_SECRET_ACCESS_KEY = os.getenv('S3_SECRET_ACCESS_KEY')
        self.S3_BUCKET_NAME = os.getenv('S3_BUCKET_NAME')
        self.S3_CUSTOM_DOMAIN = os.getenv('S3_CUSTOM_DOMAIN')
        if not self.S3_BUCKET_NAME:
            raise ValueError("S3_BUCKET_NAME is not set (check dist/.env)")
        self.s3 = boto3.client(
            's3',
            endpoint_url=self.S3_ENDPOINT_URL,
            aws_access_key_id=self.S3_ACCESS_KEY_ID,
            aws_secret_access_key=self.S3_SECRET_ACCESS_KEY,
            config=Config(
                signature_version='s3v4', 
                request_checksum_calculation = 'WHEN_REQUIRED', 
                response_checksum_validation = 'WHEN_REQUIRED'
                )  # 使用S3兼容签名版本
        )
        try:
            self.s3.head_bucket(Bucket=self.S3_BUCKET_NAME)
        except ClientError as e:
            error_code = e.response["Error"]["Code"]
            if error_code in ["404", "NoSuchBucket"]:
                self.s3.create_bucket(Bucket=self.S3_BUCKET_NAME)
            else:
                print(f"检查或创建 Bucket 时发生错误: {e}")    


    def upload_file_to_r2(self, file_path, file_key):
        try:
            # 上传文件
            if file_path and 'http' in file_path:
                # 如果文件路径是URL
                response = requests.get(file_path, headers={
                    'accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7',
                    'user-agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36'
                })
                file_data = response.content
                logger.info(f"upload to '{self.S3_ENDPOINT_URL}'")
                self.s3.upload_fileobj(BytesIO(file_data), self.S3_BUCKET_NAME, file_key)
            else:
                self.s3.upload_file(file_path, self.S3_BUCKET_NAME, file_key)

            logger.info(f"文件 '{file_path}' 成功上传到 '{self.S3_BUCKET_NAME}/{file_key}'")
            # if os.path.exists(file_path):
                # os.remove(file_path)

            file_url = self._public_object_url(file_key)
            logger.info(f"文件URL: {file_url}")
            return file_url
        except Exception as e:
            logger.info(f"上传文件过程中发生错误: {e}")
            return None

    def _public_object_url(self, file_key: str) -> str:
        """Build a public GET URL; do not double-prefix https://."""
        if self.S3_CUSTOM_DOMAIN:
            base = self.S3_CUSTOM_DOMAIN.strip().rstrip("/")
            if not base.startswith("http://") and not base.startswith("https://"):
                base = f"https://{base}"
            # OCI native style: {custom}/b/{bucket}/o/{key} when custom already ends with /b
            # Current .env uses .../n/{ns}/b + /{bucket}/o/{key}
            return f"{base}/{self.S3_BUCKET_NAME}/o/{file_key}"
        endpoint = (self.S3_ENDPOINT_URL or "").rstrip("/")
        return f"{endpoint}/{self.S3_BUCKET_NAME}/{file_key}"

    def generate_thumbnail_image(self, image_key):
        # 下载图像文件
        response = self.s3.get_object(Bucket=self.S3_BUCKET_NAME, Key=image_key)
        image_data = response['Body'].read()

        # 使用Pillow库打开图像
        image = Image.open(BytesIO(image_data))

        # 将图像缩放为50%
        width, height = image.size
        new_width = int(width * 0.5)
        new_height = int(height * 0.5)
        resized_image = image.resize((new_width, new_height))

        # 创建一个BytesIO对象来保存缩略图
        thumbnail_buffer = BytesIO()
        resized_image.save(thumbnail_buffer, format='webp')
        thumbnail_buffer.seek(0)

        # 将缩略图上传回S3
        thumbnail_key = image_key.replace('.webp', '-t.webp')
        self.s3.put_object(Bucket=self.S3_BUCKET_NAME, Key=thumbnail_key, Body=thumbnail_buffer)

        # 如果提供了自定义域名
        if self.S3_CUSTOM_DOMAIN:
            base = self.S3_CUSTOM_DOMAIN.strip().rstrip("/")
            if not base.startswith("http://") and not base.startswith("https://"):
                base = f"https://{base}"
            file_url = f"{base}/{thumbnail_key}"
        else:
            file_url = f"{self.S3_ENDPOINT_URL}/{self.S3_BUCKET_NAME}/{thumbnail_key}"
        logger.info(f"缩略图文件URL: {file_url}")
        return file_url
