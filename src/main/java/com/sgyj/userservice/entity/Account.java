package com.sgyj.userservice.entity;

import static jakarta.persistence.FetchType.LAZY;

import com.sgyj.userservice.common.UpdatedEntity;
import com.sgyj.userservice.enums.AccountRole;
import com.sgyj.userservice.security.Jwt;
import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.ws.rs.BadRequestException;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.crypto.password.PasswordEncoder;

@Entity
@Getter
@Builder
@AllArgsConstructor @NoArgsConstructor
@NamedEntityGraph(
        name ="Account.withAccountRoles",
        attributeNodes = {
                @NamedAttributeNode( "roles" )
        }
)
public class Account extends UpdatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="account_id")
    private Long id;

    private String accountNo;

    @Column(unique = true)
    private String email;

    /** 사용자 이름 */
    private String userName;

    /** 비밀번호 */
    private String password;

    @Lob @Basic
    private String emailCheckToken;

    /** 권한 */
    @ElementCollection(fetch = LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "account_roles", joinColumns = @JoinColumn(name = "account_id"))
    @Builder.Default
    private Set<AccountRole> roles = Set.of(AccountRole.USER);

    /** 이메일 인증 여부 */
    private boolean emailVerified;

    /** 최고 관리자 여부 */
    @Builder.Default
    @ColumnDefault(value="false")
    private boolean superAdmin = false;

    /** 가입일자 */
    private LocalDateTime joinedAt;

    /** 로그인 횟수 */
    private int loginCount;

    /** 로그인 실패 회수 */
    private int loginFailCount;

    /** 마지막 로그인 일자 */
    private LocalDateTime lastLoginAt;

    /** 프로필 이미지 */
    @Lob @Basic
    private String profileImage;


    /** 로그인 */
    public void login( PasswordEncoder passwordEncoder, String credentials) {
        if (!passwordEncoder.matches(credentials, this.password)) {
            this.loginFailCount++;
            throw new BadRequestException( "" );
        }
    }

    /** 로그인 후 세팅 */
    public void afterLoginSuccess () {
        this.loginFailCount = 0;
        this.loginCount++;
        this.lastLoginAt = LocalDateTime.now();
    }

    /** 권한 세팅 */
    public void setUserRole ( boolean admin ) {
        if(admin) {
            this.roles = Set.of(AccountRole.USER, AccountRole.ADMIN);
        }else {
            this.roles = Set.of(AccountRole.USER);
        }
    }

    /* 이메일 인증 토큰 세팅 */
    public void generateEmailToken ( Jwt jwt ) {
        Jwt.Claims claims = Jwt.Claims.of(this.id, this.accountNo, this.email, this.roles.stream().map( AccountRole::name ).toArray(String[]::new));
        this.emailCheckToken = jwt.createEmailToken(claims);
    }

    /** 이메일 토큰 유효성 체크 */
    public boolean isValidEmailToken ( Jwt jwt, String token ) {
        return jwt.validateToken( token );
    }

    /** 로그인 완료 */
    public void completeSignUp () {
        this.emailVerified = true;
        this.joinedAt = LocalDateTime.now();
    }

    /** 비밀번호 변경 */
    public void changePassword ( String password ) {
        this.password = password;
    }

    /** 프로필 사진 변경 */
    public void changeProfileImage ( String profileImage ) {
        this.profileImage = profileImage;
    }

}
