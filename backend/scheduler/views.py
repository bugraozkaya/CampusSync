from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from django.contrib.auth import get_user_model # Burayı değiştirdik
from django.utils.text import slugify
import random
import string

# Modellerini ve Serializerlarını import et
from .models import Institution, Department, Course, Schedule
from .serializers import (
    InstitutionSerializer, 
    UserSerializer, 
    DepartmentSerializer, 
    CourseSerializer, 
    ScheduleSerializer
)

# Aktif kullanıcı modelini al (scheduler.User modelini otomatik bulur)
User = get_user_model()

# --- YARDIMCI FONKSİYONLAR ---

def create_lecturer_account(full_name):
    # Unvan temizleme
    titles = ["Assoc. Prof.", "Assist. Prof.", "Prof.", "Dr."]
    clean_name = full_name
    for title in titles:
        clean_name = clean_name.replace(title, "")
    
    # Kullanıcı adı: halit_bakir [cite: 27]
    username = slugify(clean_name.strip()).replace("-", "_")
    password = ''.join(random.choices(string.digits, k=6))
    
    if not User.objects.filter(username=username).exists():
        # Kullanıcıyı oluştur
        user = User.objects.create_user(username=username, password=password)
        user.first_name = clean_name.strip()
        
        # MODEL KONTROLÜ: Senin modelinde 'position' veya 'role' alanı varsa set et
        # Eğer bu alanlar yoksa hata vermemesi için hasattr kullanıyoruz
        if hasattr(user, 'position'):
            user.position = 'Lecturer' # Belgedeki 'Lecturer' kuralı [cite: 54]
        elif hasattr(user, 'role'):
            user.role = 'Lecturer'
            
        user.save()
        return username, password
    return username, "Mevcut"

class CourseViewSet(viewsets.ModelViewSet):
    queryset = Course.objects.all()
    serializer_class = CourseSerializer

    @action(detail=False, methods=['post'], url_path='bulk-import')
    def bulk_import(self, request):
        try:
            import_data = request.data
            results = []
            for item in import_data:
                lecturer_name = item.get('lecturer')
                course_code = item.get('code')
                course_name = item.get('name')

                # Hesap oluştur [cite: 26]
                username, password = create_lecturer_account(lecturer_name)
                
                # Ders kaydı (Kendi modelindeki alanlara göre uyarla)
                # Course.objects.update_or_create(...) gibi bir işlem buraya eklenebilir
                
                results.append({
                    "course": course_code,
                    "user": username,
                    "pass": password
                })
            return Response(results, status=status.HTTP_201_CREATED)
        except Exception as e:
            # Hatayı terminale yazdır ki ne olduğunu görelim
            print(f"HATA OLUŞTU: {str(e)}")
            return Response({"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

# --- VIEWSETS ---

class InstitutionViewSet(viewsets.ModelViewSet):
    queryset = Institution.objects.all()
    serializer_class = InstitutionSerializer

class UserViewSet(viewsets.ModelViewSet):
    # HATA BURADAYDI: Swapped model olduğu için User.objects artık buradan çalışır
    queryset = User.objects.all() 
    serializer_class = UserSerializer

class DepartmentViewSet(viewsets.ModelViewSet):
    queryset = Department.objects.all()
    serializer_class = DepartmentSerializer

class CourseViewSet(viewsets.ModelViewSet):
    queryset = Course.objects.all()
    serializer_class = CourseSerializer

    @action(detail=False, methods=['post'], url_path='bulk-import')
    def bulk_import(self, request):
        """
        Excel/TXT listesini işler ve otomatik hesapları açar[cite: 19, 26].
        """
        import_data = request.data
        results = []

        for item in import_data:
            lecturer_name = item.get('lecturer')
            course_code = item.get('code')
            
            username, password = create_lecturer_account(lecturer_name)
            
            results.append({
                "course": course_code,
                "lecturer": lecturer_name,
                "generated_user": username,
                "generated_pass": password
            })

        return Response(results, status=status.HTTP_201_CREATED)

class ScheduleViewSet(viewsets.ModelViewSet):
    queryset = Schedule.objects.all()
    serializer_class = ScheduleSerializer