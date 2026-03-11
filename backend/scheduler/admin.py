from django.contrib import admin
from django.contrib.auth.admin import UserAdmin as BaseUserAdmin
from .models import Institution, User, Department, Semester, Classroom, Course, Schedule

class BaseMultiTenantAdmin(admin.ModelAdmin):
    """
    Tüm admin sınıfları için giriş yapmış kullanıcıları 
    ve kurumlarını kontrol eden güvenli motor.
    """
    def get_queryset(self, request):
        qs = super().get_queryset(request)
        
        # GÜVENLİK: Kullanıcı giriş yapmamışsa (login sayfası vb.) boş dön
        if not request.user.is_authenticated:
            return qs.none()
            
        # Eğer SUPERADMIN ise her şeyi görsün
        if request.user.role == 'SUPERADMIN':
            return qs
            
        # Kurum Yöneticisi ise sadece kendi kurumunun verilerini görsün
        if hasattr(self.model, 'institution'):
            return qs.filter(institution=request.user.institution)
        return qs

    def save_model(self, request, obj, form, change):
        # Kayıt esnasında güvenli rol kontrolü
        if request.user.is_authenticated and request.user.role == 'ADMIN':
            if hasattr(obj, 'institution') and not obj.institution:
                obj.institution = request.user.institution
        super().save_model(request, obj, form, change)


@admin.register(Institution)
class InstitutionAdmin(admin.ModelAdmin):
    list_display = ('name', 'institution_type')
    
    def has_module_permission(self, request):
        if not request.user.is_authenticated:
            return False
        return request.user.role == 'SUPERADMIN'


# DİKKAT: Artık Django'nun kendi BaseUserAdmin sınıfından miras alıyor!
@admin.register(User)
class UserAdmin(BaseUserAdmin):
    list_display = ('username', 'role', 'institution', 'is_staff', 'is_superuser')
    list_filter = ('role', 'institution', 'is_staff')
    
    # Standart şifre/kullanıcı ekranlarına bizim yeni eklediğimiz özellikleri (Role, Institution) dahil ediyoruz
    fieldsets = BaseUserAdmin.fieldsets + (
        ('CampusSync Yetkileri', {'fields': ('role', 'institution', 'is_approved')}),
    )
    add_fieldsets = BaseUserAdmin.add_fieldsets + (
        ('CampusSync Yetkileri', {'fields': ('role', 'institution', 'is_approved')}),
    )

    # Multi-tenant mantığını User modeline özel olarak manuel uyguluyoruz
    def get_queryset(self, request):
        qs = super().get_queryset(request)
        if not request.user.is_authenticated:
            return qs.none()
        if request.user.role == 'SUPERADMIN':
            return qs
        return qs.filter(institution=request.user.institution)

    def save_model(self, request, obj, form, change):
        if request.user.is_authenticated and request.user.role == 'ADMIN':
            if not obj.institution:
                obj.institution = request.user.institution
        super().save_model(request, obj, form, change)


@admin.register(Department, Semester, Classroom)
class GeneralAdmin(BaseMultiTenantAdmin):
    list_display = ('name', 'institution')


@admin.register(Course)
class CourseAdmin(admin.ModelAdmin):
    list_display = ('course_code', 'course_name', 'department')
    
    def get_queryset(self, request):
        qs = super().get_queryset(request)
        if not request.user.is_authenticated:
            return qs.none()
        if request.user.role == 'SUPERADMIN': 
            return qs
        return qs.filter(department__institution=request.user.institution)


@admin.register(Schedule)
class ScheduleAdmin(admin.ModelAdmin):
    list_display = ('course', 'day_of_week', 'start_time')
    
    def get_queryset(self, request):
        qs = super().get_queryset(request)
        if not request.user.is_authenticated:
            return qs.none()
        if request.user.role == 'SUPERADMIN': 
            return qs
        return qs.filter(course__department__institution=request.user.institution)