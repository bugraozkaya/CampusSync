from django.contrib import admin
from django.contrib.auth.admin import UserAdmin as BaseUserAdmin
from .models import Institution, User, Department, Classroom, Course, Schedule, Unavailability, AuditLog


class InstitutionAdmin(admin.ModelAdmin):
    list_display = ('name', 'institution_type', 'created_at')
    list_filter = ('institution_type',)
    search_fields = ('name',)

    def has_module_permission(self, request):
        if not request.user.is_authenticated:
            return False
        return request.user.role == 'SUPERADMIN'


@admin.register(User)
class UserAdmin(BaseUserAdmin):
    list_display = ('username', 'first_name', 'last_name', 'title', 'role',
                    'institution', 'must_change_password', 'is_approved')
    list_filter = ('role', 'institution', 'must_change_password')
    search_fields = ('username', 'first_name', 'last_name', 'email')

    fieldsets = BaseUserAdmin.fieldsets + (
        ('CampusSync', {'fields': (
            'role', 'title', 'institution', 'department',
            'must_change_password', 'phone', 'is_approved',
            'failed_login_attempts', 'locked_until',
        )}),
    )
    add_fieldsets = BaseUserAdmin.add_fieldsets + (
        ('CampusSync', {'fields': (
            'role', 'title', 'institution', 'department',
            'must_change_password', 'phone', 'is_approved',
        )}),
    )

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


@admin.register(Institution)
class InstitutionAdminReg(InstitutionAdmin):
    pass


@admin.register(Department)
class DepartmentAdmin(admin.ModelAdmin):
    list_display = ('name', 'institution')
    list_filter = ('institution',)
    search_fields = ('name',)

    def get_queryset(self, request):
        qs = super().get_queryset(request)
        if not request.user.is_authenticated:
            return qs.none()
        if request.user.role == 'SUPERADMIN':
            return qs
        if request.user.institution:
            return qs.filter(institution=request.user.institution)
        return qs.none()


@admin.register(Classroom)
class ClassroomAdmin(admin.ModelAdmin):
    list_display = ('room_code', 'classroom_type', 'capacity', 'institution')
    list_filter = ('classroom_type', 'institution')
    search_fields = ('room_code',)

    def get_queryset(self, request):
        qs = super().get_queryset(request)
        if not request.user.is_authenticated:
            return qs.none()
        if request.user.role == 'SUPERADMIN':
            return qs
        if request.user.institution:
            return qs.filter(institution=request.user.institution)
        return qs.none()


@admin.register(Course)
class CourseAdmin(admin.ModelAdmin):
    list_display = ('course_code', 'course_name', 'department', 'has_lab',
                    'weekly_hours', 'lab_hours')
    list_filter = ('has_lab', 'department__institution')
    search_fields = ('course_code', 'course_name')

    def get_queryset(self, request):
        qs = super().get_queryset(request)
        if not request.user.is_authenticated:
            return qs.none()
        if request.user.role == 'SUPERADMIN':
            return qs
        return qs.filter(department__institution=request.user.institution)


@admin.register(Schedule)
class ScheduleAdmin(admin.ModelAdmin):
    list_display = ('course', 'session_type', 'lecturer', 'classroom',
                    'day_of_week', 'start_time', 'end_time')
    list_filter = ('day_of_week', 'session_type', 'course__department__institution')
    search_fields = ('course__course_code', 'course__course_name',
                     'lecturer__username', 'classroom__room_code')

    def get_queryset(self, request):
        qs = super().get_queryset(request)
        if not request.user.is_authenticated:
            return qs.none()
        if request.user.role == 'SUPERADMIN':
            return qs
        return qs.filter(course__department__institution=request.user.institution)


@admin.register(Unavailability)
class UnavailabilityAdmin(admin.ModelAdmin):
    list_display = ('lecturer', 'day', 'hour')
    list_filter = ('day',)
    search_fields = ('lecturer__username',)


@admin.register(AuditLog)
class AuditLogAdmin(admin.ModelAdmin):
    list_display = ('created_at', 'actor', 'action', 'model_name', 'object_id', 'ip_address')
    list_filter = ('action', 'model_name')
    search_fields = ('actor__username', 'description')
    readonly_fields = ('actor', 'action', 'model_name', 'object_id', 'description', 'ip_address', 'created_at')

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False
