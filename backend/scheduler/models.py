from django.db import models
from django.contrib.auth.models import AbstractUser
import uuid as uuid_module


class Institution(models.Model):
    INSTITUTION_TYPES = (
        ('university', 'Üniversite'),
        ('high_school', 'Lise'),
        ('college', 'Yüksekokul'),
        ('vocational', 'Meslek Okulu'),
        ('other', 'Diğer'),
    )
    name = models.CharField(max_length=255, verbose_name="Kurum Adı")
    institution_type = models.CharField(
        max_length=20, choices=INSTITUTION_TYPES, default='university',
        verbose_name="Kurum Tipi"
    )
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return self.name

    class Meta:
        verbose_name = "Kurum"
        verbose_name_plural = "Kurumlar"


class Department(models.Model):
    institution = models.ForeignKey(
        Institution, on_delete=models.CASCADE, related_name='departments'
    )
    name = models.CharField(max_length=255)

    def __str__(self):
        return f"{self.name} — {self.institution.name}"

    class Meta:
        verbose_name = "Bölüm"
        verbose_name_plural = "Bölümler"
        unique_together = ('institution', 'name')


class User(AbstractUser):
    ROLE_CHOICES = (
        ('SUPERADMIN', 'Sistem Yöneticisi'),
        ('ADMIN', 'Kurum Yöneticisi'),
        ('STAFF', 'Personel'),
        ('IT', 'IT Personeli'),
        ('LECTURER', 'Öğretim Görevlisi'),
        ('STUDENT', 'Öğrenci'),
    )
    institution = models.ForeignKey(
        Institution, on_delete=models.SET_NULL, null=True, blank=True,
        verbose_name="Kurum"
    )
    department = models.ForeignKey(
        Department, on_delete=models.SET_NULL, null=True, blank=True,
        verbose_name="Bölüm"
    )
    role = models.CharField(
        max_length=20, choices=ROLE_CHOICES, default='LECTURER',
        verbose_name="Rol"
    )
    title = models.CharField(
        max_length=50, blank=True,
        verbose_name="Unvan"
    )  # Prof., Doç., Dr., Öğr. Gör., Arş. Gör.
    must_change_password = models.BooleanField(
        default=False, verbose_name="İlk Girişte Şifre Değiştirmeli"
    )
    phone = models.CharField(max_length=20, blank=True, verbose_name="Telefon")
    failed_login_attempts = models.PositiveSmallIntegerField(default=0)
    locked_until = models.DateTimeField(null=True, blank=True, verbose_name="Kilitli Kalma Süresi")
    is_approved = models.BooleanField(default=True, verbose_name="Onaylandı")
    fcm_token = models.CharField(max_length=500, blank=True, null=True)

    def __str__(self):
        name = self.get_full_name()
        label = f"{self.title} {name}".strip() if self.title else name
        return f"{label or self.username} ({self.role})"

    class Meta:
        verbose_name = "Kullanıcı"
        verbose_name_plural = "Kullanıcılar"


class Classroom(models.Model):
    CLASSROOM_TYPES = (
        ('LECTURE', 'Derslik'),
        ('LAB', 'Laboratuvar'),
        ('COMPUTER_LAB', 'Bilgisayar Laboratuvarı'),
        ('SEMINAR', 'Seminer Salonu'),
        ('OTHER', 'Diğer'),
    )
    institution = models.ForeignKey(
        Institution, on_delete=models.CASCADE, verbose_name="Kurum"
    )
    room_code = models.CharField(max_length=50, verbose_name="Oda Kodu")
    capacity = models.IntegerField(default=30, verbose_name="Kapasite")
    classroom_type = models.CharField(
        max_length=20, choices=CLASSROOM_TYPES, default='LECTURE',
        verbose_name="Sınıf Tipi"
    )

    class Meta:
        unique_together = ('institution', 'room_code')
        verbose_name = "Sınıf / Lab"
        verbose_name_plural = "Sınıflar / Lablar"

    def __str__(self):
        return f"{self.room_code} ({self.get_classroom_type_display()}, {self.capacity} kişi)"


class Course(models.Model):
    department = models.ForeignKey(
        Department, on_delete=models.CASCADE, verbose_name="Bölüm"
    )
    course_code = models.CharField(max_length=20, verbose_name="Ders Kodu")
    course_name = models.CharField(max_length=255, verbose_name="Ders Adı")
    has_lab = models.BooleanField(default=False, verbose_name="Laboratuvar Bileşeni Var")
    weekly_hours = models.IntegerField(default=2, verbose_name="Haftalık Ders Saati")
    lab_hours = models.IntegerField(default=0, verbose_name="Haftalık Lab Saati")

    class Meta:
        unique_together = ('department', 'course_code')
        verbose_name = "Ders"
        verbose_name_plural = "Dersler"

    def __str__(self):
        return f"{self.course_code} — {self.course_name}"


class Schedule(models.Model):
    DAY_CHOICES = (
        ('Monday', 'Pazartesi'),
        ('Tuesday', 'Salı'),
        ('Wednesday', 'Çarşamba'),
        ('Thursday', 'Perşembe'),
        ('Friday', 'Cuma'),
    )
    SESSION_TYPES = (
        ('LECTURE', 'Ders'),
        ('LAB', 'Laboratuvar'),
    )
    course = models.ForeignKey(
        Course, on_delete=models.CASCADE, verbose_name="Ders"
    )
    lecturer = models.ForeignKey(
        User, on_delete=models.SET_NULL, null=True, blank=True,
        related_name='schedules', verbose_name="Hoca / Görevli"
    )
    classroom = models.ForeignKey(
        Classroom, on_delete=models.SET_NULL, null=True, blank=True,
        verbose_name="Sınıf / Lab"
    )
    day_of_week = models.CharField(
        max_length=10, choices=DAY_CHOICES, verbose_name="Gün"
    )
    start_time = models.TimeField(verbose_name="Başlangıç Saati")
    end_time = models.TimeField(verbose_name="Bitiş Saati")
    session_type = models.CharField(
        max_length=10, choices=SESSION_TYPES, default='LECTURE',
        verbose_name="Oturum Tipi"
    )
    is_deleted = models.BooleanField(default=False, verbose_name="Silindi")
    deleted_at = models.DateTimeField(null=True, blank=True, verbose_name="Silinme Tarihi")

    class Meta:
        verbose_name = "Program"
        verbose_name_plural = "Programlar"

    def __str__(self):
        return f"{self.course.course_code} — {self.day_of_week} {self.start_time}"


class Unavailability(models.Model):
    """Öğretim görevlisinin müsait olmadığı zaman dilimleri."""
    lecturer = models.ForeignKey(
        User, on_delete=models.CASCADE, related_name='unavailabilities'
    )
    day = models.CharField(max_length=20, verbose_name="Gün")
    hour = models.CharField(max_length=15, verbose_name="Saat Dilimi")  # "08:00-10:00"

    class Meta:
        unique_together = ('lecturer', 'day', 'hour')
        verbose_name = "Müsait Değil"
        verbose_name_plural = "Müsaitlik Dışı Durumlar"

    def __str__(self):
        return f"{self.lecturer.username} — {self.day} {self.hour}"


class StudentEnrollment(models.Model):
    student = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name='enrollments',
        limit_choices_to={'role': 'STUDENT'}
    )
    course = models.ForeignKey(Course, on_delete=models.CASCADE, related_name='enrollments')
    enrolled_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ('student', 'course')
        ordering = ['-enrolled_at']

    def __str__(self):
        return f"{self.student.username} → {self.course.course_code}"


class Announcement(models.Model):
    AUDIENCE_CHOICES = [
        ('ALL', 'Tüm Kullanıcılar'),
        ('LECTURER', 'Öğretim Üyeleri'),
        ('STUDENT', 'Öğrenciler'),
        ('ADMIN', 'Yöneticiler'),
    ]
    title       = models.CharField(max_length=200)
    body        = models.TextField()
    audience    = models.CharField(max_length=20, choices=AUDIENCE_CHOICES, default='ALL')
    institution = models.ForeignKey(Institution, on_delete=models.CASCADE, related_name='announcements')
    created_by  = models.ForeignKey(User, on_delete=models.SET_NULL, null=True, related_name='announcements_created')
    created_at  = models.DateTimeField(auto_now_add=True)
    is_active   = models.BooleanField(default=True)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f"[{self.institution.name}] {self.title}"


class UserNotification(models.Model):
    user         = models.ForeignKey(User, on_delete=models.CASCADE, related_name='notifications')
    announcement = models.ForeignKey(Announcement, on_delete=models.CASCADE, related_name='user_notifications')
    is_read      = models.BooleanField(default=False)
    read_at      = models.DateTimeField(null=True, blank=True)

    class Meta:
        unique_together = ('user', 'announcement')
        ordering = ['-announcement__created_at']

    def __str__(self):
        return f"{self.user.username} – {self.announcement.title}"


class AuditLog(models.Model):
    ACTION_CHOICES = [
        ('CREATE', 'Oluşturma'),
        ('UPDATE', 'Güncelleme'),
        ('DELETE', 'Silme'),
        ('LOGIN',  'Giriş'),
        ('LOGIN_FAIL', 'Başarısız Giriş'),
    ]
    actor      = models.ForeignKey('User', on_delete=models.SET_NULL, null=True, blank=True, related_name='audit_logs')
    action     = models.CharField(max_length=20, choices=ACTION_CHOICES)
    model_name = models.CharField(max_length=100, blank=True)
    object_id  = models.CharField(max_length=50, blank=True)
    description = models.TextField(blank=True)
    ip_address = models.GenericIPAddressField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']
        verbose_name = "Denetim Kaydı"
        verbose_name_plural = "Denetim Kayıtları"

    def __str__(self):
        actor_str = self.actor.username if self.actor else 'system'
        return f"[{self.action}] {actor_str} — {self.description[:60]}"


class AttendanceSession(models.Model):
    schedule    = models.ForeignKey(Schedule, on_delete=models.CASCADE, related_name='attendance_sessions')
    token       = models.UUIDField(default=uuid_module.uuid4, editable=False, unique=True)
    created_by  = models.ForeignKey(User, on_delete=models.CASCADE, related_name='attendance_sessions_created')
    created_at  = models.DateTimeField(auto_now_add=True)
    expires_at  = models.DateTimeField()
    is_active   = models.BooleanField(default=True)
    session_date = models.DateField()

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f"{self.schedule.course.course_code} – {self.session_date} [{self.token}]"

    def is_expired(self):
        from django.utils import timezone
        return timezone.now() > self.expires_at


class AttendanceRecord(models.Model):
    session    = models.ForeignKey(AttendanceSession, on_delete=models.CASCADE, related_name='records')
    student    = models.ForeignKey(User, on_delete=models.CASCADE, related_name='attendance_records',
                                   limit_choices_to={'role': 'STUDENT'})
    checked_in_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ('session', 'student')
        ordering = ['checked_in_at']

    def __str__(self):
        return f"{self.student.username} @ {self.session}"


# ── Chat ──────────────────────────────────────────────────────────
class ChatMessage(models.Model):
    sender      = models.ForeignKey(User, on_delete=models.CASCADE, related_name='sent_messages')
    receiver    = models.ForeignKey(User, on_delete=models.CASCADE, related_name='received_messages')
    content     = models.TextField()
    created_at  = models.DateTimeField(auto_now_add=True)
    is_read     = models.BooleanField(default=False)
    read_at     = models.DateTimeField(null=True, blank=True)

    class Meta:
        ordering = ['created_at']

    def __str__(self):
        return f"{self.sender.username} → {self.receiver.username}: {self.content[:40]}"


# ── Course Materials ──────────────────────────────────────────────
class CourseMaterial(models.Model):
    MATERIAL_TYPES = [
        ('LECTURE_NOTES', 'Ders Notu'),
        ('ASSIGNMENT',    'Ödev'),
        ('EXAM',          'Sınav'),
        ('RESOURCE',      'Kaynak'),
        ('OTHER',         'Diğer'),
    ]
    course       = models.ForeignKey(Course, on_delete=models.CASCADE, related_name='materials')
    uploaded_by  = models.ForeignKey(User, on_delete=models.CASCADE, related_name='uploaded_materials')
    title        = models.CharField(max_length=200)
    description  = models.TextField(blank=True)
    file         = models.FileField(upload_to='course_materials/%Y/%m/')
    material_type = models.CharField(max_length=20, choices=MATERIAL_TYPES, default='LECTURE_NOTES')
    created_at   = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f"{self.course.course_code} – {self.title}"


# ── Grade Book ────────────────────────────────────────────────────
class Grade(models.Model):
    GRADE_TYPES = [
        ('MIDTERM', 'Vize'),
        ('FINAL',   'Final'),
        ('QUIZ',    'Quiz'),
        ('HW',      'Ödev'),
        ('LAB',     'Lab'),
        ('OTHER',   'Diğer'),
    ]
    student    = models.ForeignKey(User, on_delete=models.CASCADE, related_name='grades',
                                   limit_choices_to={'role': 'STUDENT'})
    course     = models.ForeignKey(Course, on_delete=models.CASCADE, related_name='grades')
    grade_type = models.CharField(max_length=20, choices=GRADE_TYPES, default='MIDTERM')
    score      = models.DecimalField(max_digits=5, decimal_places=2)
    max_score  = models.DecimalField(max_digits=5, decimal_places=2, default=100)
    graded_by  = models.ForeignKey(User, on_delete=models.SET_NULL, null=True,
                                   related_name='graded_entries')
    notes      = models.TextField(blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        unique_together = ('student', 'course', 'grade_type')
        ordering = ['-created_at']

    def __str__(self):
        return f"{self.student.username} | {self.course.course_code} | {self.grade_type}: {self.score}"
