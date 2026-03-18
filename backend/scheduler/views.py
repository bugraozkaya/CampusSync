from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from django.contrib.auth import get_user_model
from django.utils.text import slugify
from django.contrib.auth.hashers import make_password
import openpyxl
from rest_framework.parsers import MultiPartParser
import random
import string
from datetime import time

from rest_framework_simplejwt.views import TokenObtainPairView
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer

from .models import Institution, Department, Course, Schedule, Unavailability, Classroom, Semester
from .serializers import (
    InstitutionSerializer, 
    UserSerializer, 
    DepartmentSerializer, 
    CourseSerializer, 
    ScheduleSerializer
)

User = get_user_model()

# --- CUSTOM LOGIN ---
class MyTokenObtainPairSerializer(TokenObtainPairSerializer):
    def validate(self, attrs):
        data = super().validate(attrs)
        data['role'] = self.user.role
        data['username'] = self.user.username
        data['institution_name'] = self.user.institution.name if self.user.institution else "Sistem Geneli"
        return data

class MyTokenObtainPairView(TokenObtainPairView):
    serializer_class = MyTokenObtainPairSerializer

# --- VIEWSETS ---

class InstitutionViewSet(viewsets.ModelViewSet):
    queryset = Institution.objects.all()
    serializer_class = InstitutionSerializer

    def get_queryset(self):
        user = self.request.user
        if not user.is_authenticated: return Institution.objects.none()
        if user.role == 'SUPERADMIN':
            return Institution.objects.all()
        return Institution.objects.filter(id=user.institution_id)

class UserViewSet(viewsets.ModelViewSet):
    queryset = User.objects.all()
    serializer_class = UserSerializer

    def get_queryset(self):
        user = self.request.user
        if not user.is_authenticated:
            return User.objects.none()

        # 1. SÜPER ADMIN her şeyi görebilir
        if user.role == 'SUPERADMIN':
            return User.objects.all()

        # 2. ÜNİVERSİTE ADMİNİ filtrelemesi
        if user.role == 'ADMIN' and user.institution:
            # Sadece kendi kurumundakileri getir VE Süper Adminleri listeden çıkar
            return User.objects.filter(institution=user.institution).exclude(role='SUPERADMIN')

        # 3. Diğer durumlarda (Lecturer/Student) sadece kendilerini görebilirler
        return User.objects.filter(id=user.id)

    @action(detail=False, methods=['post'], url_path='create-admin')
    def create_admin(self, request):
        if request.user.role != 'SUPERADMIN':
            return Response({"error": "Sadece süper admin hesap açabilir."}, status=403)
        data = request.data
        try:
            institution = Institution.objects.get(id=data.get('institution_id'))
            user = User.objects.create(
                username=data.get('username'),
                first_name=data.get('first_name'),
                role='ADMIN',
                institution=institution,
                password=make_password(data.get('password'))
            )
            return Response({"message": f"{institution.name} için admin oluşturuldu."}, status=201)
        except Exception as e:
            return Response({"error": str(e)}, status=400)

class DepartmentViewSet(viewsets.ModelViewSet):
    queryset = Department.objects.all()
    serializer_class = DepartmentSerializer

    def get_queryset(self):
        user = self.request.user
        if not user.is_authenticated: return Department.objects.none()
        if user.role == 'SUPERADMIN': return Department.objects.all()
        if user.institution:
            return Department.objects.filter(institution=user.institution)
        return Department.objects.none()

class CourseViewSet(viewsets.ModelViewSet):
    queryset = Course.objects.all()
    serializer_class = CourseSerializer

    def get_queryset(self):
        user = self.request.user
        if not user.is_authenticated: return Course.objects.none()
        if user.role == 'SUPERADMIN': return Course.objects.all()
        if user.institution:
            return Course.objects.filter(department__institution=user.institution)
        return Course.objects.none()

    @action(detail=False, methods=['post'], url_path='bulk-import-excel', parser_classes=[MultiPartParser])
    def bulk_import_excel(self, request):
        user = request.user
        if user.role not in ['ADMIN', 'SUPERADMIN'] or not user.institution:
            return Response({"error": "Yetkisiz işlem."}, status=403)

        file_obj = request.FILES.get('file')
        if not file_obj: return Response({"error": "Dosya yok."}, status=400)

        results = []
        try:
            wb = openpyxl.load_workbook(file_obj, data_only=True)
            sheet = wb.active
            for row in sheet.iter_rows(min_row=2, values_only=True):
                if not any(row): continue
                dept_name, course_code, course_name, lecturer_name = row[0], row[1], row[2], row[3]
                department, _ = Department.objects.get_or_create(name=dept_name, institution=user.institution)

                titles = ["Dr.", "Prof.", "Doç.", "Öğr. Gör."]
                clean_name = str(lecturer_name)
                for t in titles: clean_name = clean_name.replace(t, "")

                username = slugify(clean_name.strip()).replace("-", "_")
                lecturer, created = User.objects.get_or_create(
                    username=username,
                    defaults={
                        'first_name': clean_name.strip(),
                        'role': 'LECTURER',
                        'institution': user.institution,
                        'password': make_password(''.join(random.choices(string.digits, k=6)))
                    }
                )

                Course.objects.update_or_create(
                    course_code=course_code, department=department,
                    defaults={'course_name': course_name, 'lecturer': lecturer}
                )
                results.append({"course": course_code, "lecturer": lecturer_name})
            return Response(results, status=201)
        except Exception as e:
            return Response({"error": str(e)}, status=500)

class ScheduleViewSet(viewsets.ModelViewSet):
    queryset = Schedule.objects.all()
    serializer_class = ScheduleSerializer

    def get_queryset(self):
        user = self.request.user
        if not user.is_authenticated: return Schedule.objects.none()
        if user.role == 'SUPERADMIN': return Schedule.objects.all()
        if user.institution:
            return Schedule.objects.filter(course__department__institution=user.institution)
        return Schedule.objects.none()

    @action(detail=False, methods=['post'], url_path='generate-auto')
    def generate_auto(self, request):
        return Response({"message": "Program oluşturuldu."})

class UnavailabilityViewSet(viewsets.ViewSet):
    def list(self, request):
        data = Unavailability.objects.filter(lecturer=request.user).values('day', 'hour')
        return Response(list(data))

    @action(detail=False, methods=['post'])
    def sync(self, request):
        Unavailability.objects.filter(lecturer=request.user).delete()
        to_create = [Unavailability(lecturer=request.user, day=s.get('day'), hour=s.get('hour')) for s in request.data]
        Unavailability.objects.bulk_create(to_create)
        return Response({"message": "Başarılı"})
